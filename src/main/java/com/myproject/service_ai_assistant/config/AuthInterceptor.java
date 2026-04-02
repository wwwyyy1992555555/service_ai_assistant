package com.myproject.service_ai_assistant.config;

import com.myproject.service_ai_assistant.exception.BusinessException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.concurrent.TimeUnit;

/**
 * 认证拦截器 - 实现 Token 验证和单设备登录控制
 */
@Slf4j
@Component
public class AuthInterceptor implements HandlerInterceptor {

    @Autowired
    private StringRedisTemplate redisTemplate;

    /**
     * Redis Key 前缀：用户当前有效的 Token
     */
    private static final String USER_TOKEN_KEY_PREFIX = "user:token:";

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        // 1. 获取请求头中的 Token
        String token = request.getHeader("Authorization");
        
        // 2. 处理 Bearer 格式
        if (token != null && token.startsWith("Bearer ")) {
            token = token.substring(7);
        }
        
        // 3. 验证 Token 是否有效
        if (!StringUtils.hasText(token)) {
            log.warn("【认证失败】Token 为空：uri={}", request.getRequestURI());
            throw new BusinessException(401, "未授权访问");
        }
        
        // 4. 从 Redis 查询 Token 对应的用户 ID
        String tokenKey = "token:" + token;
        String userId = redisTemplate.opsForValue().get(tokenKey);
        
        if (userId == null) {
            log.warn("【认证失败】Token 无效或已过期：token={}", token);
            throw new BusinessException(401, "登录已过期，请重新登录");
        }
        
        // 5. 【单设备登录控制】验证是否是当前最新的 Token
        String userTokenKey = USER_TOKEN_KEY_PREFIX + userId;
        String currentToken = redisTemplate.opsForValue().get(userTokenKey);
        
        if (!token.equals(currentToken)) {
            log.warn("【认证失败】Token 已失效（可能已在其他设备登录）：userId={}, currentToken={}, requestToken={}", 
                    userId, currentToken, token);
            throw new BusinessException(401, "您的账号已在其他设备登录，请重新登录");
        }
        
        // 6. 将用户 ID 存入请求上下文
        request.setAttribute("userId", userId);
        
        // 7. 刷新 Token 有效期（自动续期）
        redisTemplate.expire(tokenKey, 7 * 24 * 60 * 60, TimeUnit.SECONDS);
        redisTemplate.expire(userTokenKey, 7 * 24 * 60 * 60, TimeUnit.SECONDS);
        
        log.debug("【认证成功】userId={}, uri={}", userId, request.getRequestURI());
        
        return true;
    }
}
