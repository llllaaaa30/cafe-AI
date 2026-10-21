package cn.sg.intelligentcustomerservice.infrastructure.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.tool.execution.DefaultToolExecutionExceptionProcessor;
import org.springframework.ai.tool.execution.ToolExecutionExceptionProcessor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.List;

/**
 * LLM 大语言模型配置
 *
 * ChatClient 会通过 Spring AI 自动注入 ZhipuAIChatModel（由 application.yml 中 spring.ai.zhipuai 配置驱动）
 * 模型和温度等参数在 application.yml 中统一管理，无需在代码中硬编码
 */
@Slf4j
@Configuration
public class LLMConfiguration {

    /**
     * 创建 ChatClient Bean
     * Spring AI 自动配置会根据 application.yml 中的 spring.ai.zhipuai.chat.options 设置模型和参数
     *
     * @param chatClientBuilder Spring AI提供的ChatClient构建器（自动注入与ChatModel的绑定关系）
     * @return 配置完成的ChatClient实例
     */
    @Bean
    public ChatClient chatClient(ChatClient.Builder chatClientBuilder) {
        return chatClientBuilder.build();
    }

    /**
     * 工具调用异常处理器
     * 当LLM调用的Tool出现异常时，不抛出异常而是返回友好提示
     */
    @Bean
    @ConditionalOnMissingBean
    public ToolExecutionExceptionProcessor toolExecutionExceptionProcessor() {
        List<Class<? extends RuntimeException>> exceptions = new ArrayList<>();
        return DefaultToolExecutionExceptionProcessor.builder().alwaysThrow(false)
                .rethrowExceptions(exceptions)
                .build();
    }
}