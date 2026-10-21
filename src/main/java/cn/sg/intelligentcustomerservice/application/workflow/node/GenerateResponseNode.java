package cn.sg.intelligentcustomerservice.application.workflow.node;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.action.NodeAction;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;

import java.util.Map;

/**
 * 响应生成节点
 * 根据意图类型使用不同的Prompt策略：
 *   商品咨询 → 注入RAG检索到的知识库内容，要求基于事实回答
 *   闲聊     → 不使用知识库，自由对话，保持友好亲切的咖啡店店员风格
 *   兜底     → 同商品咨询
 */
@Slf4j
public class GenerateResponseNode implements NodeAction {

    private final ChatClient chatClient;

    public GenerateResponseNode(ChatClient chatClient) {
        this.chatClient = chatClient;
    }

    @Override
    public Map<String, Object> apply(OverAllState state) throws Exception {
        Map<String, Object> stateData = state.data();
        String userInput = (String) stateData.getOrDefault("userInput", "");
        String knowledge = (String) stateData.getOrDefault("knowledge", "");
        String history = (String) stateData.getOrDefault("history", "");
        String intent = (String) stateData.getOrDefault("intent", "商品咨询");

        String prompt = buildPromptByIntent(intent, userInput, knowledge, history);

        log.info("GenerateResponseNode 准备Prompt，意图: {}，Prompt长度: {}", intent, prompt.length());

        // 不再调用LLM，只将构建好的Prompt和意图放入state，由上层服务决定全量/流式调用
        return Map.of("prompt", prompt, "intent", intent);
    }

    /**
     * 根据意图构建不同的Prompt
     * 商品咨询：注入知识库 + 约束基于事实回答
     * 闲聊：不使用知识库，保持亲切友好的咖啡店店员人设
     */
    private String buildPromptByIntent(String intent, String userInput, String knowledge, String history) {
        if ("闲聊".equals(intent)) {
            return String.format("""
                    你是一家咖啡店的智能客服助手，性格亲切友好。用户正在和你闲聊。

                    历史对话：
                    %s

                    用户消息：
                    %s

                    要求：
                    1. 用轻松友好的语气回复，像朋友聊天一样
                    2. 回复简洁，不要太长
                    3. 如果用户问好，热情回应并可以顺便提一下店里的咖啡
                    """, history, userInput);
        }

        // 商品咨询 或 默认兜底：使用知识库的Prompt
        return String.format("""
                你是一个专业的咖啡智能客服，根据以下信息回答用户问题：

                知识库：
                %s

                历史对话：
                %s

                用户当前问题：
                %s

                要求：
                1. 回答必须基于知识库内容，不要编造信息
                2. 如果知识库中没有相关信息，回答"抱歉，我暂时无法回答这个问题"
                3. 保持回答简洁明了，使用友好的语气
                """, knowledge, history, userInput);
    }
}