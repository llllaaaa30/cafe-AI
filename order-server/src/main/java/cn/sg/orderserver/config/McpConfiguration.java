
package cn.sg.orderserver.config;


import cn.sg.orderserver.mcp.OrderMcpController;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * MCP（Model Context Protocol）配置
 * 将OrderMcpController中标记了@Tool注解的方法注册为MCP工具，
 * 使得AI Agent可以通过MCP协议直接调用订单相关的业务操作
 *
 * Created on 2025/10/10.
 */
@Slf4j
@Configuration
public class McpConfiguration {

    /**
     * 注册MCP工具回调提供者
     * 扫描OrderMcpController中所有@Tool方法，暴露为MCP工具供AI Agent调用
     *
     * @param orderMcpController 订单MCP控制器
     * @return 工具回调提供者
     */
    @Bean
    public ToolCallbackProvider mcpTools(OrderMcpController orderMcpController) {
        return MethodToolCallbackProvider.builder()
                .toolObjects(orderMcpController)
                .build();
    }
}