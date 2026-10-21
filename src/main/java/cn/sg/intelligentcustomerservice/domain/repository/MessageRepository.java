package cn.sg.intelligentcustomerservice.domain.repository;

import cn.sg.intelligentcustomerservice.domain.entity.Message;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * 消息仓储接口
 * 基于Spring Data JPA，提供消息实体的数据库访问能力
 */
public interface MessageRepository extends JpaRepository<Message, Long> {

    /**
     * 根据用户ID查询该用户的所有对话消息
     * @param userId 用户ID
     * @return 该用户的消息列表
     */
    List<Message> findAllByUserId(String userId);

    /**
     * 根据用户ID查询该用户的所有对话消息，按创建时间升序排列
     * 用于滑动窗口记忆——按时间顺序分组对话轮次
     *
     * @param userId 用户ID
     * @return 按时间升序的消息列表
     */
    List<Message> findAllByUserIdOrderByCreateTimeAsc(String userId);
}