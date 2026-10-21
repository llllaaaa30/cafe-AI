
package cn.sg.intelligentcustomerservice.application.workflow;

import cn.sg.intelligentcustomerservice.application.workflow.node.GenerateResponseNode;
import cn.sg.intelligentcustomerservice.application.workflow.node.IntentRecognitionNode;
import cn.sg.intelligentcustomerservice.application.workflow.node.KnowledgeRetrievalNode;
import cn.sg.intelligentcustomerservice.application.workflow.node.OrderHandlerNode;
import cn.sg.intelligentcustomerservice.domain.service.MessageDomainService;
import cn.sg.intelligentcustomerservice.infrastructure.client.OrderClient;
import com.alibaba.cloud.ai.graph.*;
import com.alibaba.cloud.ai.graph.action.AsyncNodeAction;
import com.alibaba.cloud.ai.graph.exception.GraphStateException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.*;

import static com.alibaba.cloud.ai.graph.action.AsyncEdgeAction.edge_async;


/**
 * 智能客服工作流配置（含意图识别）
 * 使用Spring AI Alibaba Graph的StateGraph定义客服对话工作流：
 *
 *                        ┌── "商品咨询" → knowledgeRetrieval → generateResponse ──┐
 *   START → intentRecognition ──┤                                                    ├── END
 *                        ├── "下单/查订单/取消订单" → orderHandler ──────────────┤
 *                        └── "闲聊" → generateResponse ─────────────────────────┘
 *
 * 意图识别使用LLM进行轻量级分类，根据意图分流到不同的处理节点
 */
@Slf4j
@Configuration
public class IntelligentCustomerServiceWorkflowConfig {

    /**
     * 注册智能客服工作流Bean
     * 构建带意图识别的StateGraph：
     *   节点0 intentRecognition：LLM分类用户意图（商品咨询/下单/查订单/取消订单/闲聊）
     *   节点1 knowledgeRetrieval：向量检索知识库（仅商品咨询走此路径）
     *   节点2 generateResponse：LLM生成回答（商品咨询和闲聊走此路径）
     *   节点3 orderHandler：处理订单操作（下单/查订单/取消订单走此路径）
     *
     * @param chatClient           LLM对话客户端
     * @param vectorStore           Redis向量存储
     * @param messageDomainService 消息领域服务
     * @param orderClient          order-server Feign客户端
     * @return 编译后的可执行Graph
     */
    @Bean("intelligentCustomerService")
    public CompiledGraph intelligentCustomerServiceWorkflow(ChatClient chatClient,
                                                             VectorStore vectorStore,
                                                             MessageDomainService messageDomainService,
                                                             OrderClient orderClient) throws GraphStateException {
        // 定义全局状态的Key策略工厂
        KeyStrategyFactory overAllStateFactory = () -> {
            OverAllState state = new OverAllState();
            return state.keyStrategies();
        };
        StateGraph stateGraph = new StateGraph("intelligentCustomerService", overAllStateFactory);

        // 节点0: 意图识别 — 用小Prompt让LLM快速分类用户意图
        IntentRecognitionNode intentRecognitionNode = new IntentRecognitionNode(chatClient);
        stateGraph.addNode("intentRecognition", AsyncNodeAction.node_async(intentRecognitionNode));

        // 节点1: 向量检索知识 — 仅商品咨询路径使用
        KnowledgeRetrievalNode knowledgeRetrievalNode = new KnowledgeRetrievalNode(vectorStore);
        stateGraph.addNode("knowledgeRetrieval", AsyncNodeAction.node_async(knowledgeRetrievalNode));

        // 节点2: LLM生成回答 — 商品咨询和闲聊共用，根据intent使用不同Prompt
        GenerateResponseNode generateResponseNode = new GenerateResponseNode(chatClient);
        stateGraph.addNode("generateResponse", AsyncNodeAction.node_async(generateResponseNode));

        // 节点3: 订单处理 — 处理下单/查订单/取消订单，通过Feign调用order-server
        OrderHandlerNode orderHandlerNode = new OrderHandlerNode(orderClient, chatClient);
        stateGraph.addNode("orderHandler", AsyncNodeAction.node_async(orderHandlerNode));

        // ========== 边定义 ==========

        // START → 意图识别
        stateGraph.addEdge(StateGraph.START, "intentRecognition");

        // 意图识别 → 条件路由（根据intent值分流到不同节点）
        stateGraph.addConditionalEdges("intentRecognition",
                edge_async(state -> {
                    String intent = (String) state.value("intent").orElse("商品咨询");
                    log.info("Workflow 条件路由，意图: {}", intent);
                    return switch (intent) {
                        case "下单", "查订单", "取消订单" -> "orderHandler";
                        case "闲聊" -> "generateResponse";
                        default -> "knowledgeRetrieval";
                    };
                }),
                Map.of(
                        "orderHandler", "orderHandler",
                        "generateResponse", "generateResponse",
                        "knowledgeRetrieval", "knowledgeRetrieval"
                ));

        // 知识检索 → 回答生成（商品咨询路径）
        stateGraph.addEdge("knowledgeRetrieval", "generateResponse");

        // 订单处理 → END
        stateGraph.addEdge("orderHandler", StateGraph.END);

        // 回答生成 → END
        stateGraph.addEdge("generateResponse", StateGraph.END);

        return stateGraph.compile();
    }


}