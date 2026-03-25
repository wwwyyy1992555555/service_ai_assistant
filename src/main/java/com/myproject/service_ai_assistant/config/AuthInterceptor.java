package com.myproject.service_ai_assistant.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * 认证拦截器
 * TODO: 实现 Token 验证逻辑
 * TODO: 从请求头获取 Token: Authorization: Bearer <token>
 * TODO: 验证 Token 有效性（Redis 中是否存在、是否过期）
 * TODO: 解析 Token 获取用户信息，存入请求上下文
 * TODO: 排除登录接口、静态资源等不需要认证的路径
 */
@Slf4j
@Component
public class AuthInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        // TODO: 实现 Token 验证逻辑
        // 1. 获取请求头中的 Token
        // String token = request.getHeader("Authorization");
        
        // 2. 验证 Token 格式（去掉 "Bearer " 前缀）
        // if (token != null && token.startsWith("Bearer ")) {
        //     token = token.substring(7);
        // }
        
        // 3. 从 Redis 验证 Token 是否有效
        // String userId = redisTemplate.opsForValue().get("token:" + token);
        // if (userId == null) {
        //     throw new BusinessException(401, "登录已过期，请重新登录");
        // }
        
        // 4. 将用户信息存入请求上下文
        // request.setAttribute("userId", userId);
        
        return true;
    }
}
