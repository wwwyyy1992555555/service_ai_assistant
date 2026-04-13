package com.myproject.service_ai_assistant.interceptor;

import com.myproject.service_ai_assistant.common.LevelCode;
import com.myproject.service_ai_assistant.common.ResultCode;
import com.myproject.service_ai_assistant.context.UserContext;
import com.myproject.service_ai_assistant.exception.BusinessException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * 角色权限校验拦截器
 * 防止垂直越权：低权限用户访问高权限接口
 */
@Slf4j
@Component
public class RoleValidationInterceptor implements HandlerInterceptor {

    /**
     * 需要管理员权限的路径前缀
     */
    private static final String[] ADMIN_PATHS = {
            "/api/user/",      // 用户管理
            "/api/tenant/",    // 租户管理
            "/api/settings/"   // 系统配置
    };

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String uri = request.getRequestURI();
        
        // 1. 判断是否需要校验（只校验管理接口）
        if (!needRoleValidation(uri)) {
            return true;
        }

        // 2. 从 UserContext 获取用户信息（AuthInterceptor 已设置）
        Long userId = UserContext.getUserId();
        Long tenantId = UserContext.getTenantId();
        Integer roleLevel = UserContext.getRoleLevel();
        
        if (userId == null || tenantId == null || roleLevel == null) {
            log.warn("【权限校验失败】用户信息缺失：userId={}, tenantId={}, roleLevel={}, uri={}", 
                    userId, tenantId, roleLevel, uri);
            throw new BusinessException(ResultCode.UNAUTHORIZED);
        }

        // 3. 运营商（tenant_id=0）拥有所有权限，直接通过
        if (tenantId == LevelCode.ROLE_LEVEL_TENANT_ID) {
            log.debug("【权限校验通过】运营商：userId={}, tenantId=0, uri={}", userId, uri);
            return true;
        }

        // 4. 特殊处理：租户管理接口只允许 tenant_id==0 的用户访问（上面已处理，此处为兜底）
        if (uri.startsWith("/api/tenant/")) {
            log.warn("【权限校验失败】非超级租户尝试访问租户管理：userId={}, tenantId={}, roleLevel={}, uri={}", 
                    userId, tenantId, roleLevel, uri);
            throw new BusinessException(ResultCode.PERMISSION_DENIED);
        }

        // 5. 普通用户校验 roleLevel：操作员(roleLevel=2)禁止访问管理接口
        if (roleLevel > 1) {
            log.warn("【权限校验失败】权限不足：userId={}, tenantId={}, roleLevel={}, uri={}", 
                    userId, tenantId, roleLevel, uri);
            throw new BusinessException(ResultCode.PERMISSION_DENIED);
        }

        log.debug("【权限校验通过】userId={}, tenantId={}, roleLevel={}, uri={}", userId, tenantId, roleLevel, uri);
        return true;
    }

    /**
     * 判断是否需要进行角色校验
     */
    private boolean needRoleValidation(String uri) {
        for (String path : ADMIN_PATHS) {
            if (uri.startsWith(path)) {
                return true;
            }
        }
        return false;
    }
}
