package cn.sg.intelligentcustomerservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

/**
 * 智能客服服务启动类
 * 启用OpenFeign客户端支持，用于调用order-server微服务
 */
@EnableFeignClients
@SpringBootApplication
public class IntelligentCustomerServiceApplication {

    /**
     * 应用入口
     * @param args 命令行参数
     */
    public static void main(String[] args) {
        SpringApplication.run(IntelligentCustomerServiceApplication.class, args);
    }

}