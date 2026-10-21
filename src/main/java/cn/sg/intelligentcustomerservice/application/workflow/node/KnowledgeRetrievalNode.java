package cn.sg.intelligentcustomerservice.application.workflow.node;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.action.NodeAction;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 知识检索节点
 * 负责从向量数据库中检索与用户输入相关的知识
 */
@Slf4j
public class KnowledgeRetrievalNode implements NodeAction {

    private final VectorStore vectorStore;

    public KnowledgeRetrievalNode(VectorStore vectorStore) {
        this.vectorStore = vectorStore;
    }

    @Override
    public Map<String, Object> apply(OverAllState state) throws Exception {
        // 从state中获取用户输入
        Map<String, Object> stateData = state.data();
        String userInput = (String) stateData.getOrDefault("userInput", "");
        
        // 向量检索相关知识
        SearchRequest build = SearchRequest.builder().query(userInput)
                .similarityThreshold(0.1)
                .topK(3)
                .build();
        List<Document> documents = vectorStore.similaritySearch(build);
        String knowledge = documents.stream()
                .map(doc -> {
                    Object answer = doc.getMetadata().get("answer");
                    return answer != null ? answer.toString() : doc.getText();
                })
                .collect(Collectors.joining("\n"));
        
        log.info("向量检索到的知识: {}", knowledge);
        return Map.of("knowledge", knowledge);
    }
}