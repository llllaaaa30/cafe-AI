package cn.sg.orderserver.mcp;

import cn.sg.orderserver.dto.CreateOrderCmd;
import cn.sg.orderserver.dto.CreateOrderResponse;
import cn.sg.orderserver.entity.Order;
import cn.sg.orderserver.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

import java.util.List;

/**
 * 订单MCP控制器
 * 通过MCP协议将订单相关功能暴露为AI Agent可调用的工具(Tool)
 * 每个@Tool方法代表了AI Agent可以直接执行的业务操作
 * 注意：当前使用标准Spring组件，MCP集成需要额外配置
 */
@Slf4j
@Component
@RequiredArgsConstructor
@Validated
public class OrderMcpController {
    private final OrderService orderService;

    /**
     * 创建订单工具
     * AI Agent可通过此工具为用户下单
     */
    @Tool(description = "下单:通过用户ID、商品价格,商品名称创建一笔订单，返回订单详情")
    public String createOrder(@ToolParam(description = "用户ID")String userId,
                              @ToolParam(description = "用户姓名")String userName,
                              @ToolParam(description = "用户手机号")String userPhone,
                              @ToolParam(description = "商品价格")String price,
                              @ToolParam(description = "商品名称")String itemName) {
        log.info("收到MCP创建订单请求，用户ID {}", userId);
        CreateOrderCmd request = new CreateOrderCmd(userId,userName,userPhone,price,itemName);
         return orderService.createOrder(request);
    }

    /**
     * 订单详情查询工具
     * AI Agent可通过此工具根据订单号查询订单详情
     */
    @Tool(description = "订单详情:通过订单号查询订单详情")
    public String orderDetail(@ToolParam(description = "订单ID")String orderNumber) {
        log.info("orderDetail start，orderNumber: {}", orderNumber);
        String detail = orderService.orderDetail(orderNumber);
        log.info("orderDetail end，orderNumber: {}", orderNumber);
        return detail;
    }

    /**
     * 用户订单查询工具
     * AI Agent可通过此工具查询某个用户的历史订单
     */
    @Tool(description = "用户订单查询:查询某个用户下有哪些订单")
    public List<Order> userOrder(@ToolParam(description = "用户ID")String userId) {
        log.info("userOrder start，userId: {}", userId);
        List<Order> orders = orderService.userOrder(userId);
        log.info("userOrder end，orderNumber");
        return orders;
    }

    /**
     * 订单取消工具
     * AI Agent可通过此工具取消指定订单
     */
    @Tool(description = "订单取消:通过订单号执行订单取消操作")
    public String orderCancel(@ToolParam(description = "订单ID")String orderNumber) {
        log.info("orderDetail start，orderNumber: {}", orderNumber);
        String result = orderService.cancel(orderNumber);
        log.info("orderDetail end，orderNumber: {}", orderNumber);
        return result;
    }
}