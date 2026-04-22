package com.myproject.service_ai_assistant.interceptor;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.HandlerInterceptor;

import com.myproject.service_ai_assistant.common.ResultCode;
import com.myproject.service_ai_assistant.context.UserContext;
import com.myproject.service_ai_assistant.exception.BusinessException;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;

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
        
        // 8. 【运营商权限限制】tenant_id=0 只能访问租户管理和用户管理相关接口
        if (tenantId == 0) {
            String uri = request.getRequestURI();
            if (!isProviderAllowedUri(uri)) {
                log.warn("【权限拒绝】运营商无权访问该接口：userId={}, uri={}", userId, uri);
                throw new BusinessException(ResultCode.PERMISSION_DENIED);
            }
        }
        
        log.debug("【认证成功】userId={}, tenantId={}, roleLevel={}, uri={}", 
                userId, tenantId, roleLevel, request.getRequestURI());
        
        return true;
    }
    
    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        // 清理 UserContext，防止内存泄漏
        UserContext.clear();
    }
    
    /**
     * 判断运营商（tenant_id=0）是否有权访问该接口
     * 运营商只能访问：租户管理、用户管理相关接口
     */
    private boolean isProviderAllowedUri(String uri) {
        // 租户管理接口
        if (uri.startsWith("/api/tenant/")) {
            return true;
        }
        
        // 用户管理接口
        if (uri.startsWith("/api/user/")) {
            return true;
        }
        
        // 认证相关接口（登录、登出、获取用户信息）
        if (uri.startsWith("/api/auth/")) {
            return true;
        }
        
        // 其他接口一律拒绝
        return false;
    }
}
