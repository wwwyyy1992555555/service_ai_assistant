package com.myproject.service_ai_assistant;

import lombok.extern.slf4j.Slf4j;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * AI 智库企业咨询平台 - 启动类
 */
@Slf4j
@SpringBootApplication
@MapperScan("com.myproject.service_ai_assistant.mapper")
public class ServiceAiAssistantApplication {

    public static void main(String[] args) {
        SpringApplication.run(ServiceAiAssistantApplication.class, args);
        log.info("====================================");
        log.info("AI 智库企业咨询平台启动成功！");
        log.info("API 文档地址：http://localhost:8080/swagger-ui.html");
        log.info("管理后台：http://localhost:8080/admin.html");
        log.info("聊天页面：http://localhost:8080/chat.html");
        log.info("后台登陆：http://localhost:8080/login.html");
        log.info("====================================");
    }

}
