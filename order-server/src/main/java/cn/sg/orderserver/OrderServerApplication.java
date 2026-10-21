package cn.sg.orderserver;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 订单服务启动类
 * 基于Spring Boot框架，提供订单管理功能（创建、查询、取消、完成）
 * 通过两种方式暴露服务接口：
 *   1. REST API（OrderController）—— 供智能客服服务通过Feign调用
 *   2. MCP协议（OrderMcpController）—— 供AI Agent直接调用
 * 运行在8083端口，使用MySQL持久化存储订单数据
 */
@SpringBootApplication
public class OrderServerApplication {

    /**
     * 应用程序入口，启动订单服务
     * System.setProperty优先级高于application.yml，确保端口不被主项目配置覆盖
     * @param args 命令行参数
     */
    public static void main(String[] args) {
        System.setProperty("server.port", "8083");
        System.setProperty("server.servlet.context-path", "");
        SpringApplication.run(OrderServerApplication.class, args);
    }
}