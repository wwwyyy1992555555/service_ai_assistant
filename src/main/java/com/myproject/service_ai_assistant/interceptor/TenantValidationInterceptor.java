package com.myproject.service_ai_assistant.interceptor;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.myproject.service_ai_assistant.common.ResultCode;
import com.myproject.service_ai_assistant.common.LevelCode;
import com.myproject.service_ai_assistant.entity.TenantInfo;
import com.myproject.service_ai_assistant.exception.BusinessException;
import com.myproject.service_ai_assistant.service.TenantInfoService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * 租户校验拦截器
 * 统一拦截需要租户隔离的接口，校验租户状态
 */
@Slf4j
@Component
public class TenantValidationInterceptor implements HandlerInterceptor {

    @Autowired
    private TenantInfoService tenantInfoService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 无需校验租户的接口路径
     */
    private static final Set<String> EXCLUDE_PATHS = new HashSet<>(Arrays.asList(
            "/api/auth/login",      // 登录接口
            "/api/auth/logout"      // 登出接口
    ));

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String uri = request.getRequestURI();
        String method = request.getMethod();

        // 1. 判断是否需要校验
        if (!needValidation(uri, method)) {
            return true;
        }

        // 2. 提取 tenantId
        Long tenantId = extractTenantId(request);

        if (tenantId == null) {
            log.warn("【租户校验失败】缺少租户 ID：uri={}", uri);
            throw new BusinessException(ResultCode.TENANT_ID_REQUIRED);
        }

        // 3. 运营商（tenant_id=0）无需校验租户状态
        if (tenantId == LevelCode.ROLE_LEVEL_TENANT_ID) {
            log.debug("【租户校验跳过】运营商：tenantId=0, uri={}", uri);
            return true;
        }

        // 4. 校验普通租户状态
        TenantInfo tenant = tenantInfoService.getById(tenantId);
        if (tenant == null) {
            log.warn("【租户校验失败】租户不存在：tenantId={}, uri={}", tenantId, uri);
            throw new BusinessException(ResultCode.TENANT_NOT_FOUND);
        }

        if (tenant.getStatus() == 0) {
            log.warn("【租户校验失败】租户已禁用：tenantId={}, uri={}", tenantId, uri);
            throw new BusinessException(ResultCode.TENANT_DISABLED);
        }

        if (tenant.getExpireTime() != null && tenant.getExpireTime().isBefore(LocalDateTime.now())) {
            log.warn("【租户校验失败】租户已过期：tenantId={}, expireTime={}, uri={}", 
                    tenantId, tenant.getExpireTime(), uri);
            throw new BusinessException(ResultCode.TENANT_EXPIRED);
        }

        log.debug("【租户校验通过】tenantId={}, uri={}", tenantId, uri);
        
        return true;
    }
    
    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        // 不清理 UserContext，由 AuthInterceptor 统一清理
    }

    /**
     * 判断是否需要校验租户
     */
    private boolean needValidation(String uri, String method) {
        // 只校验 /api/ 开头的接口
        if (!uri.startsWith("/api/")) {
            return false;
        }

        // 排除特定路径
        if (EXCLUDE_PATHS.contains(uri)) {
            return false;
        }

        // 排除 OPTIONS 请求（CORS 预检）
        if ("OPTIONS".equalsIgnoreCase(method)) {
            return false;
        }

        return true;
    }

    /**
     * 从请求中提取 tenantId
     * 优先级：
     * 1. UserContext（AuthInterceptor 已设置）
     * 2. URL 参数：?tenantId=1
     */
    private Long extractTenantId(HttpServletRequest request) {
        // 方式1：从 UserContext 获取（认证后已设置）
        Long contextTenantId = com.myproject.service_ai_assistant.context.UserContext.getTenantId();
        if (contextTenantId != null) {
            return contextTenantId;
        }
        
        // 方式2：从 URL 参数获取
        String tenantIdParam = request.getParameter("tenantId");
        if (tenantIdParam != null && !tenantIdParam.isEmpty()) {
            try {
                return Long.parseLong(tenantIdParam);
            } catch (NumberFormatException e) {
                log.warn("【租户 ID 格式错误】tenantId={}", tenantIdParam);
            }
        }

        return null;
    }
}
