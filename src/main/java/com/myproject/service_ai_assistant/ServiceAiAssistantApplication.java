package com.myproject.service_ai_assistant;

import lombok.extern.slf4j.Slf4j;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * AI 智库企业咨询平台 - 启动类
 */
@Slf4j
@SpringBootApplication
@MapperScan("com.myproject.service_ai_assistant.mapper")
@EnableScheduling  // 启用定时任务支持
public class ServiceAiAssistantApplication extends SpringBootServletInitializer {

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

    @Override
    protected SpringApplicationBuilder configure(SpringApplicationBuilder builder) {
        return builder.sources(ServiceAiAssistantApplication.class);
    }

}
