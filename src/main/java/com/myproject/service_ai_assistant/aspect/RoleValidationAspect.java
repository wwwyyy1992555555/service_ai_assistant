package com.myproject.service_ai_assistant.aspect;

import com.myproject.service_ai_assistant.annotation.RequireRole;
import com.myproject.service_ai_assistant.common.LevelCode;
import com.myproject.service_ai_assistant.common.ResultCode;
import com.myproject.service_ai_assistant.exception.BusinessException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.lang.reflect.Method;

/**
 * 角色权限校验切面
 * 拦截带有 @RequireRole 注解的方法，校验用户角色权限
 */
@Slf4j
@Aspect
@Component
public class RoleValidationAspect {

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Before("@annotation(com.myproject.service_ai_assistant.annotation.RequireRole)")
    public void validateRole(JoinPoint joinPoint) {
        // 1. 获取方法上的注解
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        RequireRole requireRole = method.getAnnotation(RequireRole.class);
        
        if (requireRole == null) {
            return;
        }

        int minLevel = requireRole.minLevel();

        // 2. 获取当前请求
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes == null) {
            throw new BusinessException(ResultCode.UNAUTHORIZED);
        }
        
        HttpServletRequest request = attributes.getRequest();
        String uri = request.getRequestURI();

        // 3. 从请求头获取 Token
        String token = request.getHeader("Authorization");
        if (token != null && token.startsWith("Bearer ")) {
            token = token.substring(7);
        }

        if (token == null || token.isEmpty()) {
            log.warn("【AOP权限校验失败】Token 为空：uri={}", uri);
            throw new BusinessException(ResultCode.UNAUTHORIZED);
        }

        // 4. 从 Redis 获取用户信息（格式：userId:tenantId:roleLevel）
        String tokenKey = "token:" + token;
        String userInfo = redisTemplate.opsForValue().get(tokenKey);
        
        if (userInfo == null) {
            log.warn("【AOP权限校验失败】Token 无效：uri={}", uri);
            throw new BusinessException(ResultCode.TOKEN_EXPIRED);
        }
        
        // 5. 解析用户信息
        String[] parts = userInfo.split(":");
        if (parts.length != 3) {
            log.warn("【AOP权限校验失败】用户信息格式错误：userInfo={}, uri={}", userInfo, uri);
            throw new BusinessException(ResultCode.UNAUTHORIZED);
        }
        
        String userId = parts[0];
        Long tenantId = Long.parseLong(parts[1]);
        int roleLevel = Integer.parseInt(parts[2]);
        
        // 6. 运营商（tenant_id=0）拥有所有权限，直接通过
        if (tenantId == LevelCode.ROLE_LEVEL_TENANT_ID) {
            log.debug("【AOP权限校验通过】运营商：userId={}, tenantId=0, uri={}", userId, uri);
            return;
        }

        // 7. 普通用户校验 roleLevel
        if (roleLevel > minLevel) {
            log.warn("【AOP权限校验失败】权限不足：userId={}, tenantId={}, roleLevel={}, requiredMinLevel={}, uri={}", 
                    userId, tenantId, roleLevel, minLevel, uri);
            throw new BusinessException(ResultCode.PERMISSION_DENIED);
        }

        log.debug("【AOP权限校验通过】userId={}, tenantId={}, roleLevel={}, uri={}", userId, tenantId, roleLevel, uri);
    }
}
