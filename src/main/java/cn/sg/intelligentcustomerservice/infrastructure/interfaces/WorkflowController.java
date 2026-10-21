package cn.sg.intelligentcustomerservice.infrastructure.interfaces;

import cn.sg.intelligentcustomerservice.application.dto.WorkflowRunCMD;
import cn.sg.intelligentcustomerservice.application.service.WorkflowApplicationService;
import cn.sg.intelligentcustomerservice.common.lang.R;
import com.alibaba.fastjson2.JSONObject;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

/**
 * 工作流控制器
 * 对外暴露智能客服工作流调用接口，接收用户消息并返回AI回复
 * 路由映射：
 *   POST /workflows/run      — 流式SSE输出（推荐）
 *   POST /workflows/run/sync — 全量JSON输出（兼容旧版）
 */
@Slf4j
@RestController
@RequestMapping("/workflows")
@RequiredArgsConstructor
public class WorkflowController {
    private final WorkflowApplicationService workflowApplicationService;

    /**
     * 执行智能客服工作流（流式SSE输出）
     * 接收用户ID和输入消息，经过RAG检索后以流式方式逐token返回LLM生成内容
     *
     * @param cmd 包含userId和userInput的工作流命令
     * @return SSE流
     */
    @PostMapping(value = "/run", produces = MediaType.TEXT_PLAIN_VALUE)
    public Flux<String> run(@RequestBody WorkflowRunCMD cmd) {
        log.info("WorkflowController 流式工作流请求: {}", JSONObject.toJSONString(cmd));
        return workflowApplicationService.runStream(cmd);
    }

    /**
     * 执行智能客服工作流（全量JSON输出，兼容旧版前端）
     */
    @PostMapping("/run/sync")
    public R<String> runSync(@RequestBody WorkflowRunCMD cmd) {
        log.info("WorkflowController 全量工作流请求: {}", JSONObject.toJSONString(cmd));
        return R.success(workflowApplicationService.run(cmd));
    }

}