package cn.sg.intelligentcustomerservice.domain.entity;


import cn.sg.intelligentcustomerservice.domain.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import lombok.extern.slf4j.Slf4j;

/**
 * 消息实体
 * 映射到数据库 t_message 表，存储智能客服的对话记录
 * 每条消息由 role 字段区分是用户(user)还是AI助手(assistant)
 *
 * Created on 2025/11/8.
 */
@Slf4j
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "t_message")
public class Message extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    /**
     * 关联的用户ID，用于按用户查询对话历史
     */
    @Column(name = "user_id")
    private String userId;

    /**
     * 消息角色：user 表示用户消息，assistant 表示AI助手消息
     */
    @Column(name = "role")
    private String role;

    /**
     * 消息的文本内容
     */
    @Column(name = "content")
    private String content;

    /**
     * 创建一个用户消息实体
     * @param userId  用户ID
     * @param content 消息内容
     * @return 用户消息
     */
    public static Message ofUser(String userId, String content) {
        return Message.builder()
                .userId(userId)
                .role("user")
                .content(content)
                .build();
    }

    /**
     * 创建一个AI助手消息实体
     * @param userId  用户ID
     * @param content AI生成的回复内容
     * @return 助手消息
     */
    public static Message ofAssistant(String userId, String content) {
        return Message.builder()
                .userId(userId)
                .role("assistant")
                .content(content)
                .build();
    }
}