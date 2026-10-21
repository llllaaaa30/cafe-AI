package cn.sg.intelligentcustomerservice.application.workflow.node;

import cn.sg.intelligentcustomerservice.infrastructure.client.OrderCancelCmd;
import cn.sg.intelligentcustomerservice.infrastructure.client.OrderClient;
import cn.sg.intelligentcustomerservice.infrastructure.client.OrderDTO;
import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.action.NodeAction;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;

import java.util.List;
import java.util.Map;

/**
 * 订单处理节点
 * 根据意图类型执行对应的订单操作：
 *   下单    → 从用户输入中提取商品信息 → 调用order-server创建订单
 *   查订单  → 查询指定用户的所有订单
 *   取消订单 → 从上下文中获取订单号 → 调用order-server取消订单
 *
 * 对"下单"意图，使用LLM从用户自然语言中提取商品名称和价格，
 * 然后通过OpenFeign调用order-server的REST接口完成实际操作
 */
@Slf4j
public class OrderHandlerNode implements NodeAction {

    private final OrderClient orderClient;
    private final ChatClient chatClient;

    public OrderHandlerNode(OrderClient orderClient, ChatClient chatClient) {
        this.orderClient = orderClient;
        this.chatClient = chatClient;
    }

    @Override
    public Map<String, Object> apply(OverAllState state) throws Exception {
        Map<String, Object> stateData = state.data();
        String intent = (String) stateData.get("intent");
        String userInput = (String) stateData.getOrDefault("userInput", "");
        String userId = (String) stateData.getOrDefault("userId", "");
        String userInfo = (String) stateData.getOrDefault("user", "");

        log.info("OrderHandlerNode 开始处理订单意图: {}，用户ID: {}", intent, userId);

        String output = switch (intent) {
            case "下单"     -> handleCreateOrder(userInput, userId, userInfo);
            case "查订单"   -> handleQueryOrders(userId);
            case "取消订单" -> handleCancelOrder(userInput);
            default        -> "抱歉，订单处理遇到问题，请稍后再试";
        };

        return Map.of("output", output);
    }

    /**
     * 处理下单：从用户输入中提取商品信息 → 调用order-server创建订单
     * 先用LLM解析用户输入中的商品名称和价格，再通过Feign调用
     */
    private String handleCreateOrder(String userInput, String userId, String userInfo) {
        log.info("OrderHandlerNode 处理下单请求");

        String extractPrompt = """
                从用户输入中提取商品信息，以JSON格式返回（只返回JSON，不要其他内容）：

                用户输入：%s

                返回格式（严格JSON）：
                {"itemName": "商品名称", "price": "价格"}

                规则：
                1. 如果用户没有指定价格，根据知识库推断：美式9.9元，拿铁8.8元，其他19.9元
                2. itemName只填写商品名称本身，不要包含"一杯"等量词
                """.formatted(userInput);

        String jsonStr = chatClient.prompt(extractPrompt).call().content();
        log.info("OrderHandlerNode LLM提取结果: {}", jsonStr);

        try {
            // 解析LLM返回的JSON
            String itemName;
            String price;
            if (jsonStr != null && jsonStr.contains("itemName")) {
                com.alibaba.fastjson2.JSONObject json = com.alibaba.fastjson2.JSONObject.parseObject(jsonStr);
                itemName = json.getString("itemName");
                price = json.getString("price");
            } else {
                itemName = "未知商品";
                price = "未知";
            }

            // 从userInfo中提取用户名和手机号（格式："用户ID: xxx\n用户名：xxx\n手机号：xxx"）
            String userName = extractField(userInfo, "用户名：");
            String userPhone = extractField(userInfo, "手机号：");

            // 调用order-server创建订单
            OrderClient.CreateOrderRequest request = new OrderClient.CreateOrderRequest(
                    userId, userName, userPhone, price, itemName);
            String createResult = orderClient.createOrder(request);

            log.info("OrderHandlerNode 下单成功: {}", createResult);
            return "已为您下单成功！\n\n" + createResult;

        } catch (Exception e) {
            log.error("OrderHandlerNode 下单失败", e);
            return "抱歉，下单失败：" + e.getMessage() + "。请稍后再试或联系客服。";
        }
    }

    /**
     * 处理查订单：查询该用户的所有订单并格式化返回
     */
    private String handleQueryOrders(String userId) {
        log.info("OrderHandlerNode 查询用户订单，userId: {}", userId);

        try {
            List<OrderDTO> orders = orderClient.userOrder(new OrderClient.UserOrderRequest(userId));

            if (orders == null || orders.isEmpty()) {
                return "您目前没有订单记录。需要来一杯咖啡吗？";
            }

            StringBuilder sb = new StringBuilder("您的订单如下：\n\n");
            for (int i = 0; i < orders.size(); i++) {
                OrderDTO order = orders.get(i);
                String statusStr = switch (order.getStatus()) {
                    case 0 -> "制作中";
                    case 1 -> "已完成";
                    case 2 -> "已退款";
                    default -> "未知";
                };
                sb.append(String.format("%d. 订单号：%s | %s | ¥%s | %s\n",
                        i + 1, order.getOrderNumber(), order.getItemName(),
                        order.getPrice(), statusStr));
            }
            return sb.toString();

        } catch (Exception e) {
            log.error("OrderHandlerNode 查询订单失败", e);
            return "抱歉，查询订单失败，请稍后再试。";
        }
    }

    /**
     * 处理取消订单：从用户输入中提取订单号 → 调用order-server取消
     */
    private String handleCancelOrder(String userInput) {
        log.info("OrderHandlerNode 处理取消订单");

        String extractPrompt = """
                从用户输入中提取订单号，只返回订单号（纯文本，不要任何额外内容）。
                如果没有找到订单号，返回 "NONE"。

                用户输入：%s
                订单号：""".formatted(userInput);

        String orderNumber = chatClient.prompt(extractPrompt).call().content();

        if (orderNumber == null || orderNumber.trim().equals("NONE") || orderNumber.isBlank()) {
            return "请告诉我您要取消的订单号，例如：\"帮我取消 ORDER123456789\"";
        }

        orderNumber = orderNumber.trim();
        log.info("OrderHandlerNode 提取到订单号: {}", orderNumber);

        try {
            String result = orderClient.cancel(new OrderCancelCmd(orderNumber));
            if ("SUCCESS".equals(result)) {
                return "订单 " + orderNumber + " 已成功取消并退款。";
            }
            return result;
        } catch (Exception e) {
            log.error("OrderHandlerNode 取消订单失败", e);
            return "抱歉，取消订单失败，请稍后再试。";
        }
    }

    /**
     * 从格式化文本中提取指定字段的值
     * 例如从 "用户ID: abc\n用户名：张三\n手机号：13800138000" 中提取用户名："张三"
     */
    private String extractField(String text, String fieldName) {
        if (text == null || fieldName == null) return "未知";
        int start = text.indexOf(fieldName);
        if (start < 0) return "未知";
        start += fieldName.length();
        int end = text.indexOf("\n", start);
        if (end < 0) end = text.length();
        return text.substring(start, end).trim();
    }
}