package com.myproject.service_ai_assistant.config;

import com.myproject.service_ai_assistant.common.ResultCode;
import com.myproject.service_ai_assistant.context.UserContext;
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
            throw new BusinessException(ResultCode.UNAUTHORIZED);
        }
        
        // 4. 从 Redis 查询 Token 对应的用户信息
        String tokenKey = "token:" + token;
        String userInfo = redisTemplate.opsForValue().get(tokenKey);
        
        if (userInfo == null) {
            log.warn("【认证失败】Token 无效或已过期：token={}", token);
            throw new BusinessException(ResultCode.TOKEN_EXPIRED);
        }
        
        // 5. 解析用户信息（格式：userId:tenantId:roleLevel）
        String[] parts = userInfo.split(":");
        if (parts.length != 3) {
            log.warn("【认证失败】用户信息格式错误：userInfo={}", userInfo);
            throw new BusinessException(ResultCode.UNAUTHORIZED);
        }
        
        Long userId = Long.parseLong(parts[0]);
        Long tenantId = Long.parseLong(parts[1]);
        Integer roleLevel = Integer.parseInt(parts[2]);
        
        // 6. 【单设备登录控制】验证是否是当前最新的 Token
        String userTokenKey = USER_TOKEN_KEY_PREFIX + userId;
        String currentToken = redisTemplate.opsForValue().get(userTokenKey);
        
        if (!token.equals(currentToken)) {
            log.warn("【认证失败】Token 已失效（可能已在其他设备登录）：userId={}, currentToken={}, requestToken={}", 
                    userId, currentToken, token);
            throw new BusinessException(ResultCode.MULTI_LOGIN);
        }
        
        // 7. 将用户信息存入 UserContext（供 Service 层使用）
        UserContext.set(userId, tenantId, roleLevel);
        
        log.debug("【认证成功】userId={}, tenantId={}, roleLevel={}, uri={}", 
                userId, tenantId, roleLevel, request.getRequestURI());
        
        return true;
    }
    
    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        // 清理 UserContext，防止内存泄漏
        UserContext.clear();
    }
}
