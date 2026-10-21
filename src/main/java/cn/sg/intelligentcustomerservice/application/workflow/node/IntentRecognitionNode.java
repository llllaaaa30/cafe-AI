package cn.sg.intelligentcustomerservice.application.workflow.node;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.action.NodeAction;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;

import java.util.Map;

/**
 * 意图识别节点
 * 使用LLM对用户输入进行意图分类，将结果写入State供后续节点进行条件路由
 *
 * 支持的意图类型：
 *   商品咨询  → 走RAG知识检索 → LLM生成回答
 *   下单      → 走订单处理（调用order-server创建订单）
 *   查订单    → 走订单处理（查询用户历史订单）
 *   取消订单  → 走订单处理（取消指定订单）
 *   闲聊      → 直接走LLM生成回答（无需知识库检索）
 *
 * 工作流路由由 IntelligentCustomerServiceWorkflowConfig 中的 addConditionalEdges 控制
 */
@Slf4j
public class IntentRecognitionNode implements NodeAction {

    private final ChatClient chatClient;

    public IntentRecognitionNode(ChatClient chatClient) {
        this.chatClient = chatClient;
    }

    @Override
    public Map<String, Object> apply(OverAllState state) throws Exception {
        Map<String, Object> stateData = state.data();
        String userInput = (String) stateData.getOrDefault("userInput", "");

        log.info("IntentRecognitionNode 开始识别用户意图，输入: {}", userInput);

        String intent = classifyIntent(userInput);

        log.info("IntentRecognitionNode 识别结果: {}", intent);

        return Map.of("intent", intent);
    }

    /**
     * 使用LLM进行意图分类
     * 通过精心设计的Prompt约束LLM只返回预定义的意图标签
     *
     * @param userInput 用户输入文本
     * @return 意图标签：商品咨询/下单/查订单/取消订单/闲聊
     */
    private String classifyIntent(String userInput) {
        String prompt = """
                你是一个意图分类器。分析用户的输入，只返回以下五种意图之一（不要返回其他任何内容）：

                意图类型：
                - 商品咨询：用户询问菜单、咖啡种类、价格、口感等产品信息
                - 下单：用户明确表达购买意图，如"我要一杯美式"、"下单"、"来一杯拿铁"、"帮我点一杯"
                - 查订单：用户询问自己订单的状态、进度，如"我的订单怎么样了"、"查一下我的订单"
                - 取消订单：用户想取消已有订单，如"帮我取消订单"、"退款"
                - 闲聊：问候、感谢、告别等社交对话，如"你好"、"谢谢"、"再见"，以及不涉及咖啡业务的随意聊天

                规则：
                1. 只输出五个标签之一，不要有任何多余文字、标点或换行
                2. 提到具体咖啡名称+购买动作（如"要一杯美式"）属于下单，不是商品咨询
                3. "美式有吗"、"有什么咖啡"等纯询问属于商品咨询

                用户输入：%s
                意图：""".formatted(userInput);

        String rawIntent = chatClient.prompt(prompt).call().content();

        return normalizeIntent(rawIntent);
    }

    /**
     * 标准化LLM返回的意图标签
     * 处理LLM可能返回的带空格、换行、不精确匹配等问题
     *
     * @param rawIntent LLM原始输出
     * @return 标准化后的意图标签，兜底返回"商品咨询"
     */
    private String normalizeIntent(String rawIntent) {
        if (rawIntent == null) {
            return "商品咨询";
        }
        String cleaned = rawIntent.trim().replace("\n", "").replace("\r", "");

        if (cleaned.contains("下单")) return "下单";
        if (cleaned.contains("查订单")) return "查订单";
        if (cleaned.contains("取消订单")) return "取消订单";
        if (cleaned.contains("商品咨询")) return "商品咨询";
        if (cleaned.contains("闲聊")) return "闲聊";

        log.warn("IntentRecognitionNode 无法识别意图标签，原始输出: {}，兜底为商品咨询", rawIntent);
        return "商品咨询";
    }
}