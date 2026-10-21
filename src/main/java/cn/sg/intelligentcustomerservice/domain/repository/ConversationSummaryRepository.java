package cn.sg.intelligentcustomerservice.domain.repository;

import cn.sg.intelligentcustomerservice.domain.entity.ConversationSummary;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * 对话摘要仓储接口
 * 基于Spring Data JPA，提供对话摘要实体的数据库访问能力
 */
public interface ConversationSummaryRepository extends JpaRepository<ConversationSummary, Long> {

    /**
     * 根据用户ID查询对话摘要
     * @param userId 用户ID
     * @return 该用户的对话摘要（每个用户至多一条）
     */
    Optional<ConversationSummary> findByUserId(String userId);
}