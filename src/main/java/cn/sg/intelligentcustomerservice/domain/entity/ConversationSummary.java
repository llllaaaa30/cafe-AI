package cn.sg.intelligentcustomerservice.domain.entity;

import cn.sg.intelligentcustomerservice.domain.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

/**
 * 对话摘要实体
 * 映射到数据库 t_conversation_summary 表
 *
 * 每行存储某个用户截至某一时刻的对话压缩摘要，配合滑动窗口记忆实现增量更新：
 *   - compressed_round_count 记录已压缩的轮次数
 *   - 下次构建记忆时，只压缩新增溢出的轮次并追加到 summary_text
 *   - 当 compressed_round_count 等于溢出轮次总数时，直接复用 summary_text，无需调用 LLM
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "t_conversation_summary",
       uniqueConstraints = @UniqueConstraint(columnNames = "user_id"))
public class ConversationSummary extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    /**
     * 关联的用户ID，与 t_user.id 对应
     */
    @Column(name = "user_id", length = 64, unique = true)
    private String userId;

    /**
     * LLM 压缩生成的摘要文本
     */
    @Column(name = "summary_text", columnDefinition = "TEXT")
    private String summaryText;

    /**
     * 此摘要覆盖的对话轮次数
     * 例如 compressed_round_count=10 表示摘要包含了前10轮对话的压缩结果
     * 用于增量压缩时判断哪些轮次已经压缩过
     */
    @Column(name = "compressed_round_count")
    private Integer compressedRoundCount;
}