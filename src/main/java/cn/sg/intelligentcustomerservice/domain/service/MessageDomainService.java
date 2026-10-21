package cn.sg.intelligentcustomerservice.domain.service;


import cn.sg.intelligentcustomerservice.domain.entity.Message;
import cn.sg.intelligentcustomerservice.domain.repository.MessageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;


/**
 * 消息领域服务
 * 负责管理对话消息的持久化和历史查询
 */
@Service
@RequiredArgsConstructor
public class MessageDomainService {

    private final MessageRepository messageRepository;

    /**
     * 保存用户发送的消息
     * @param userId  用户ID
     * @param content 用户消息内容
     */
    public void addUserMsg(String userId, String content) {
        messageRepository.saveAndFlush(Message.ofUser(userId, content));
    }

    /**
     * 保存AI助手生成的回复
     * @param userId  用户ID
     * @param content AI回复内容
     */
    public void addAssistantMsg(String userId, String content) {
        messageRepository.saveAndFlush(Message.ofAssistant(userId, content));
    }

    /**
     * 获取指定用户的所有消息，按创建时间升序排列
     * 供滑动窗口记忆组件使用——需要时序信息来分组对话轮次
     *
     * @param userId 用户ID
     * @return 按时间升序的消息列表
     */
    public List<Message> findAllOrdered(String userId) {
        return messageRepository.findAllByUserIdOrderByCreateTimeAsc(userId);
    }

    /**
     * 获取指定用户的历史对话记录，拼接为Prompt可用的文本格式
     * 格式："role: content"，多条消息用换行符分隔
     *
     * @param userId 用户ID
     * @return 格式化的历史对话文本
     */
    public String history(String userId) {
        List<Message> message = messageRepository.findAllByUserId(userId);
        return Optional.ofNullable(message).orElse(Collections.emptyList()).stream()
                .map(item -> item.getRole() + ": " + item.getContent())
                .collect(Collectors.joining("\n"));
    }
}