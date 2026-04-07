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
    private AuthInterceptor authInterceptor;
    
    @Autowired
    private com.myproject.service_ai_assistant.interceptor.TenantValidationInterceptor tenantValidationInterceptor;

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
        // 1. 注册认证拦截器（优先级最高，先认证再校验权限）
        registry.addInterceptor(authInterceptor)
                .addPathPatterns("/api/**")
                .excludePathPatterns(
                        "/api/auth/login",
                        "/api/password/check-strength",
                        "/api/consult/ask",
                        "/api/consult/hot-questions",
                        "/api/consult/parse-user-input",
                        "/api/consult/feedback/submit",
                        "/api/settings/get",
                        "/api/doc.html",
                        "/swagger-ui/**",
                        "/v3/api-docs/**",
                        "/static/**",
                        "/**/*.html",
                        "/**/*.css",
                        "/**/*.js"
                );
        
        // 2. 注册租户校验拦截器（认证后执行，可从 UserContext 获取 tenantId）
        registry.addInterceptor(tenantValidationInterceptor)
                .addPathPatterns("/api/**")
                .excludePathPatterns(
                        "/api/auth/**",
                        "/api/consult/ask",
                        "/api/consult/hot-questions",
                        "/api/consult/feedback/submit",
                        "/api/doc.html",
                        "/swagger-ui/**",
                        "/v3/api-docs/**"
                );
        
        // 3. 注册请求日志拦截器
        registry.addInterceptor(requestLogInterceptor)
                .addPathPatterns("/api/**")
                .excludePathPatterns(
                        "/api/doc.html",
                        "/swagger-ui/**",
                        "/v3/api-docs/**"
                );
    }

}
