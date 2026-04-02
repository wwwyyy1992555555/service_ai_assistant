package com.myproject.service_ai_assistant.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web 配置类 - 静态资源映射 & 拦截器注册
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Autowired
    private RequestLogInterceptor requestLogInterceptor;

    @Autowired
    private AuthInterceptor authInterceptor; // TODO: 启用认证拦截器

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // 映射静态资源 (支持两种路径)
        registry.addResourceHandler("/static/**")
                .addResourceLocations("classpath:/static/")
                .setCachePeriod(0); // 开发环境：禁用缓存，每次请求最新资源
        
        // 根路径直接访问静态资源
        registry.addResourceHandler("/**")
                .addResourceLocations("classpath:/static/")
                .setCachePeriod(0); // 开发环境：禁用缓存
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // 注册请求日志拦截器
        registry.addInterceptor(requestLogInterceptor)
                .addPathPatterns("/api/**")  // 拦截所有 API 请求
                .excludePathPatterns(
                        "/api/doc.html",
                        "/swagger-ui/**",
                        "/v3/api-docs/**"
                );  // 排除文档相关请求
        
        // 注册认证拦截器，实现 Token 验证
        registry.addInterceptor(authInterceptor)
                .addPathPatterns("/api/**")  // 拦截所有 API 请求
                .excludePathPatterns(
                        "/api/auth/login",  // 排除登录接口
                        "/api/password/check-strength",  // 排除密码强度检测
                        "/api/consult/ask",  // 排除聊天接口（匿名访问）
                        "/api/consult/hot-questions",  // 排除热门问题接口（匿名访问）
                        "/api/consult/parse-user-input",  // 排除用户输入解析接口（匿名访问）
                        "/api/consult/feedback/submit",  // 排除提交反馈接口（匿名访问）
                        "/api/settings/get",  // 排除获取配置接口（匿名访问）
                        "/api/doc.html",
                        "/swagger-ui/**",
                        "/v3/api-docs/**",
                        "/static/**",
                        "/**/*.html",
                        "/**/*.css",
                        "/**/*.js"
                );  // 排除静态资源和文档
    }

}
