package cn.sg.intelligentcustomerservice.application.service;


import cn.sg.intelligentcustomerservice.application.dto.WorkflowRunCMD;
import cn.sg.intelligentcustomerservice.application.service.ConversationMemoryService.MemoryContext;
import cn.sg.intelligentcustomerservice.domain.entity.User;
import cn.sg.intelligentcustomerservice.domain.service.MessageDomainService;
import cn.sg.intelligentcustomerservice.domain.service.UserDomainService;
import com.alibaba.cloud.ai.graph.CompiledGraph;
import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.fastjson2.JSONObject;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.util.*;

/**
 * 工作流应用服务
 * 负责编排智能客服对话的完整流程：
 *   1. 通过滑动窗口构建对话记忆（最近5轮完整 + 更早压缩摘要）
 *   2. 保存用户消息
 *   3. 执行 StateGraph 工作流
 *   4. 保存AI回复
 *
 * @author thread
 */
@Slf4j
@Service
@AllArgsConstructor
public class WorkflowApplicationService {
    private final CompiledGraph graph;
    private final MessageDomainService messageDomainService;
    private final UserDomainService userDomainService;
    private final ConversationMemoryService conversationMemoryService;
    private final ChatClient chatClient;

    /**
     * 运行智能客服工作流（全量返回）
     * @param cmd 包含userId和userInput的工作流命令
     * @return AI生成的回复内容
     */
    public String run(WorkflowRunCMD cmd) {
        Map<String,Object> inputParam = JSONObject.parseObject(JSONObject.toJSONString(cmd));
        try {
            // 前置处理：滑动窗口构建对话记忆
            MemoryContext memory = conversationMemoryService.buildMemory(cmd.userId());
            inputParam.put("history", memory.toPromptText());

            // 保存用户发送的消息到数据库
            messageDomainService.addUserMsg(cmd.userId(), cmd.userInput());

            // 获取用户信息并注入到工作流上下文
            User user = userDomainService.get(cmd.userId());
            inputParam.put("user", user.toStr());

            // 调用StateGraph工作流引擎
            Optional<OverAllState> state  = graph.call(inputParam);
            Map<String, Object> result = state.map(OverAllState::data).orElse(new HashMap<>());

            // 如果Graph只准备了Prompt（GenerateResponseNode），则由本层调用LLM
            String output;
            if (result.containsKey("prompt")) {
                String prompt = (String) result.get("prompt");
                output = chatClient.prompt(prompt).call().content();
                log.info("WorkflowApplicationService 全量LLM生成完成，长度: {}", output != null ? output.length() : 0);
            } else {
                output = result.get("output").toString();
            }

            messageDomainService.addAssistantMsg(cmd.userId(), output);
            return output;
        }catch (Exception e) {
            log.error("执行工作流失败 fail,", e);
            throw new RuntimeException("系统内部错误");
        }finally {
            log.info("WorkflowController[]execute 结束工作流执行");
        }
    }

    /**
     * 运行智能客服工作流（流式返回）
     * Graph执行同上，但最终LLM调用使用流式输出，逐个token推送给前端
     *
     * @param cmd 包含userId和userInput的工作流命令
     * @return 流式输出的Flux，每个元素是一个文本块
     */
    public Flux<String> runStream(WorkflowRunCMD cmd) {
        Map<String, Object> inputParam = JSONObject.parseObject(JSONObject.toJSONString(cmd));
        try {
            // 前置处理
            MemoryContext memory = conversationMemoryService.buildMemory(cmd.userId());
            inputParam.put("history", memory.toPromptText());
            messageDomainService.addUserMsg(cmd.userId(), cmd.userInput());
            User user = userDomainService.get(cmd.userId());
            inputParam.put("user", user.toStr());

            // 执行工作流
            Optional<OverAllState> state = graph.call(inputParam);
            Map<String, Object> result = state.map(OverAllState::data).orElse(new HashMap<>());

            // 判断是LLM生成路径还是订单处理路径
            if (result.containsKey("prompt")) {
                String prompt = (String) result.get("prompt");
                log.info("WorkflowApplicationService 流式LLM生成开始");

                StringBuilder fullResponse = new StringBuilder();

                return chatClient.prompt(prompt).stream().content()
                        .doOnNext(fullResponse::append)
                        .doOnComplete(() -> {
                            messageDomainService.addAssistantMsg(cmd.userId(), fullResponse.toString());
                            log.info("WorkflowApplicationService 流式LLM生成完成，总长度: {}", fullResponse.length());
                        })
                        .doOnError(e -> {
                            log.error("WorkflowApplicationService 流式LLM生成失败", e);
                        });
            } else {
                // 订单处理路径：不需要流式，直接返回完整结果
                String output = result.get("output").toString();
                messageDomainService.addAssistantMsg(cmd.userId(), output);
                return Flux.just(output);
            }
        } catch (Exception e) {
            log.error("执行工作流失败", e);
            return Flux.just("抱歉，系统内部错误，请稍后再试");
        }
    }


}