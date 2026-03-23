package com.myproject.service_ai_assistant.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web 配置类 - 静态资源映射
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // 映射静态资源（支持两种路径）
        registry.addResourceHandler("/static/**")
                .addResourceLocations("classpath:/static/");
        
        // 根路径直接访问静态资源
//        registry.addResourceHandler("/**")
//                .addResourceLocations("classpath:/static/");
    }

}
