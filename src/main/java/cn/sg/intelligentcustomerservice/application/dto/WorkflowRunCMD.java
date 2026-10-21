package cn.sg.intelligentcustomerservice.application.dto;


import jakarta.validation.constraints.NotBlank;

/**
 * 工作流运行命令对象
 * 封装前端传入的智能客服对话请求参数
 *
 * @author thread
 */
public record WorkflowRunCMD(
        /**
         * 用户ID，用于标识对话用户和查询历史记录
         */
        @NotBlank(message = "userId is not blank")
        String userId,
        /**
         * 用户输入的问题/消息内容
         */
        @NotBlank(message = "userInput is not blank")
        String userInput) {
}