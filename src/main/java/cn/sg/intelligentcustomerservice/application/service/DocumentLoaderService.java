package cn.sg.intelligentcustomerservice.application.service;

import cn.hutool.crypto.digest.DigestUtil;
import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.annotation.ExcelProperty;
import com.google.common.collect.Lists;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Service;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * 文档加载服务
 * 实现ApplicationRunner接口，在应用启动时自动异步将CSV知识库加载到Redis向量数据库
 * 加载流程：读取qa/QA.csv → EasyExcel解析 → 每条QA转为Document → 向量化写入Redis → 供RAG检索使用
 *
 * CSV格式：question,answer
 * 向量化模型：embedding-3（智谱AI）
 * 存储引擎：Redis Stack（RediSearch模块）
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DocumentLoaderService implements ApplicationRunner {
    private final VectorStore vectorStore;

    @Value("${app.knowledge.csv-path:data/qa.csv}")
    private String csvPath;

    /**
     * 应用启动后自动异步执行文档加载，不阻塞主线程
     */
    @Override
    public void run(ApplicationArguments args) throws Exception {
        log.info("开始异步加载知识库文档...");
        loadDocumentsAsync();
    }

    /**
     * 异步加载文档到向量数据库
     */
    public void loadDocumentsAsync() {
        CompletableFuture.runAsync(() -> {
            try {
                loadDocuments();
            } catch (Exception e) {
                log.error("异步加载文档失败", e);
            }
        });
    }

    /**
     * 读取CSV文件，逐条解析为Document并批量写入Redis向量数据库
     * 采用MD5(question)作为文档ID，确保重复加载时幂等
     */
    public void loadDocuments() {
        File csvFile = new File(csvPath);
        if (!csvFile.exists()) {
            log.warn("CSV文件不存在: {}", csvPath);
            return;
        }
        log.info("开始从CSV文件加载文档: {}", csvPath);
        try {
            // 使用EasyExcel读取CSV文件，按QAData模型解析每行数据
            List<Document> docs = EasyExcel.read(csvFile)
                    .head(QAData.class)
                    .sheet()
                    .doReadSync()
                    .stream()
                    .map(item -> toDocument(item))
                    .collect(Collectors.toList());
            // 每10条为一批写入Redis，避免一次性写入过多数据
            Lists.partition(docs,10).forEach(vectorStore::add);
            log.info("从CSV文件加载文档完成 : {}", csvPath);
        } catch (Exception e) {
            log.error("加载文档过程中发生错误", e);
        }
    }

    /**
     * 将CSV中一行QA数据转换为Spring AI的Document对象
     * @param raw CSV原始行数据
     * @return 向量化的Document（content = question + answer, metadata包含question和answer）
     */
    private Document toDocument(Object raw) {
        QAData document = (QAData) raw;
        String id = DigestUtil.md5Hex(document.getQuestion());
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("id", id);
        metadata.put("question", document.getQuestion());
        metadata.put("answer", document.getAnswer());

        // 将问题和答案拼接作为向量化的内容，提高检索效果
        String content = document.getQuestion() + " " + document.getAnswer();
        return new Document(id, content, metadata);
    }

    /**
     * CSV数据模型，对应CSV中的两列：question和answer
     */
    @Data
    public static class QAData {
        @ExcelProperty("问题")
        private String question;
        @ExcelProperty("回答")
        private String answer;
    }
}