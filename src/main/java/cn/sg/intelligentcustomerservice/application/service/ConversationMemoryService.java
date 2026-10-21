package cn.sg.intelligentcustomerservice.application.service;

import cn.sg.intelligentcustomerservice.domain.entity.ConversationSummary;
import cn.sg.intelligentcustomerservice.domain.entity.Message;
import cn.sg.intelligentcustomerservice.domain.repository.ConversationSummaryRepository;
import cn.sg.intelligentcustomerservice.domain.service.MessageDomainService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * 对话记忆服务 —— 滑动窗口 + LLM增量压缩 + DB缓存
 *
 * 核心策略：
 *   1. 将数据库中所有历史消息按时间顺序分组为对话轮次（一轮 = 用户消息 + AI回复）
 *   2. 保留最近 N 轮（默认5轮）的完整内容 → 作为"短期记忆"
 *   3. N 轮之前更早的对话 → 调用LLM压缩为摘要 → 存库
 *   4. 后续请求：摘要已覆盖的轮次直接复用，仅压缩**新增溢出**的轮次
 *
 * 增量压缩示例：
 *   第6轮：溢出1轮 → 全量压缩前1轮 → 存库(compressedRoundCount=1)
 *   第7轮：溢出2轮 → 压缩第2轮 → 追加到库中摘要 → 存库(compressedRoundCount=2)
 *   第8轮：溢出3轮 → 压缩第3轮 → 追加到库中摘要 → 存库(compressedRoundCount=3)
 *   第8轮(重复调用)：溢出3轮，compressedRoundCount=3 → 命中缓存，不调LLM
 *
 * 效果：
 *   - O(1) LLM调用：每次最多只压缩1轮
 *   - O(1) Token：Prompt中历史始终为 摘要(200字) + 最近5轮
 *   - 缓存命中：同一轮次内多次请求无LLM调用
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ConversationMemoryService {

    private final MessageDomainService messageDomainService;
    private final ConversationSummaryRepository summaryRepository;
    private final ChatClient chatClient;

    /**
     * 滑动窗口大小（默认保留最近5轮完整对话）
     */
    private static final int WINDOW_SIZE = 5;

    /**
     * 记忆结构体：摘要 + 最近对话
     */
    public record MemoryContext(
            String summary,           // 压缩摘要（长期记忆），空字符串表示无需压缩
            String recentConversation // 最近N轮完整对话（短期记忆）
    ) {
        public boolean isEmpty() {
            return summary.isEmpty() && recentConversation.isEmpty();
        }

        public String toPromptText() {
            if (isEmpty()) {
                return "";
            }
            StringBuilder sb = new StringBuilder();
            if (!summary.isEmpty()) {
                sb.append("【对话摘要（更早的对话内容）】\n");
                sb.append(summary).append("\n\n");
            }
            if (!recentConversation.isEmpty()) {
                sb.append("【最近对话】\n");
                sb.append(recentConversation);
            }
            return sb.toString();
        }
    }

    /**
     * 构建对话记忆（带DB缓存 + 增量压缩）
     *
     * @param userId 用户ID
     * @return 记忆上下文（摘要 + 最近对话）
     */
    @Transactional
    public MemoryContext buildMemory(String userId) {
        List<Message> messages = messageDomainService.findAllOrdered(userId);
        if (messages == null || messages.isEmpty()) {
            return new MemoryContext("", "");
        }

        List<ConversationRound> rounds = groupIntoRounds(messages);

        // 总轮次不超过窗口大小 → 无需压缩，全部作为短期记忆
        if (rounds.size() <= WINDOW_SIZE) {
            String recent = formatRounds(rounds);
            return new MemoryContext("", recent);
        }

        // 需要压缩的轮次数
        int overflowCount = rounds.size() - WINDOW_SIZE;
        List<ConversationRound> recentRounds = rounds.subList(overflowCount, rounds.size());
        String recent = formatRounds(recentRounds);

        // 从DB加载已有的摘要缓存
        ConversationSummary cachedSummary = summaryRepository.findByUserId(userId).orElse(null);

        // 场景1：缓存命中 → 直接复用，不调LLM
        if (cachedSummary != null && cachedSummary.getCompressedRoundCount() == overflowCount) {
            log.info("ConversationMemoryService 缓存命中，userId={}，overflowCount={}", userId, overflowCount);
            return new MemoryContext(cachedSummary.getSummaryText(), recent);
        }

        // 场景2：增量压缩 → 只压缩新增溢出的轮次，追加到已有摘要
        if (cachedSummary != null && cachedSummary.getCompressedRoundCount() < overflowCount) {
            int startIndex = cachedSummary.getCompressedRoundCount();
            List<ConversationRound> newOverflowRounds = rounds.subList(startIndex, overflowCount);
            log.info("ConversationMemoryService 增量压缩，userId={}，已压缩{}轮，新增溢出{}轮",
                    userId, startIndex, newOverflowRounds.size());

            String mergedSummary = mergeSummaries(cachedSummary.getSummaryText(), newOverflowRounds);
            cachedSummary.setSummaryText(mergedSummary);
            cachedSummary.setCompressedRoundCount(overflowCount);
            summaryRepository.save(cachedSummary);

            return new MemoryContext(mergedSummary, recent);
        }

        // 场景3：无缓存 或 summary已过期（compressedRoundCount > overflowCount）
        // → 全量压缩所有溢出轮次
        List<ConversationRound> oldRounds = rounds.subList(0, overflowCount);
        log.info("ConversationMemoryService 全量压缩，userId={}，溢出轮次={}", userId, oldRounds.size());

        String summary = compressRounds(oldRounds);

        // 持久化压缩结果
        ConversationSummary toSave = cachedSummary != null ? cachedSummary : new ConversationSummary();
        toSave.setUserId(userId);
        toSave.setSummaryText(summary);
        toSave.setCompressedRoundCount(overflowCount);
        summaryRepository.save(toSave);

        return new MemoryContext(summary, recent);
    }

    /**
     * 将新增溢出轮次合并到已有摘要中（调用LLM）
     *
     * @param existingSummary 已有的摘要文本
     * @param newRounds       新增溢出的对话轮次
     * @return 合并后的完整摘要
     */
    private String mergeSummaries(String existingSummary, List<ConversationRound> newRounds) {
        String newText = formatRounds(newRounds);

        String prompt = """
                你是一个对话摘要生成器。请将以下新增对话追加到已有的摘要中。

                已有摘要：
                %s

                新增对话：
                %s

                要求：
                1. 将新增内容与已有摘要合并为一段连贯的摘要
                2. 摘要控制在200字以内
                3. 只输出摘要文本，不要有任何前缀说明
                4. 不要编造原文中没有的信息
                5. 使用中文输出""".formatted(existingSummary, newText);

        try {
            String result = chatClient.prompt(prompt).call().content();
            log.info("ConversationMemoryService 增量合并完成，新增字符数: {}", newText.length());
            return result != null ? result.trim() : existingSummary;
        } catch (Exception e) {
            log.error("ConversationMemoryService 增量合并失败，降级为追加原文", e);
            // 降级策略：用原文追加代替LLM合并
            return existingSummary + "\n" + (newText.length() > 100 ? newText.substring(0, 100) + "..." : newText);
        }
    }

    /**
     * 将消息列表按 user → assistant 顺序分组为对话轮次
     */
    private List<ConversationRound> groupIntoRounds(List<Message> messages) {
        List<ConversationRound> rounds = new ArrayList<>();

        for (int i = 0; i < messages.size(); i++) {
            Message msg = messages.get(i);
            if ("user".equals(msg.getRole())) {
                String userContent = msg.getContent();
                String assistantContent = null;
                if (i + 1 < messages.size() && "assistant".equals(messages.get(i + 1).getRole())) {
                    assistantContent = messages.get(i + 1).getContent();
                }
                rounds.add(new ConversationRound(userContent, assistantContent));
            }
        }

        return rounds;
    }

    /**
     * 使用 LLM 将多轮对话压缩为一段简洁摘要（全量压缩模式）
     */
    private String compressRounds(List<ConversationRound> rounds) {
        String rawText = formatRounds(rounds);

        String prompt = """
                你是一个对话摘要生成器。请将以下历史对话压缩为一段简洁的摘要。

                原始对话：
                %s

                要求：
                1. 提取对话中的关键信息：用户关注话题、用户偏好、已确认的事实、做出的决策
                2. 摘要控制在200字以内
                3. 只输出摘要文本，不要有任何前缀说明
                4. 不要编造原文中没有的信息
                5. 使用中文输出""".formatted(rawText);

        try {
            String summary = chatClient.prompt(prompt).call().content();
            log.info("ConversationMemoryService 全量压缩完成，原始字符数: {}，摘要字符数: {}",
                    rawText.length(), summary != null ? summary.length() : 0);
            return summary != null ? summary.trim() : "";
        } catch (Exception e) {
            log.error("ConversationMemoryService LLM压缩失败，降级为截断", e);
            return rawText.length() > 200 ? rawText.substring(0, 200) + "..." : rawText;
        }
    }

    /**
     * 将对话轮次列表格式化为文本
     */
    private String formatRounds(List<ConversationRound> rounds) {
        StringBuilder sb = new StringBuilder();
        for (ConversationRound round : rounds) {
            sb.append("user: ").append(round.userContent);
            if (round.assistantContent != null) {
                sb.append("\nassistant: ").append(round.assistantContent);
            }
            sb.append("\n");
        }
        return sb.toString();
    }

    /**
     * 对话轮次：一轮对话 = 用户消息 + AI回复
     */
    private record ConversationRound(String userContent, String assistantContent) {}
}