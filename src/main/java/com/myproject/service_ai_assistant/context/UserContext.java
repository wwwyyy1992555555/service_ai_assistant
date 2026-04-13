package com.myproject.service_ai_assistant.context;

import com.myproject.service_ai_assistant.common.LevelCode;
import com.myproject.service_ai_assistant.common.ResultCode;
import com.myproject.service_ai_assistant.exception.BusinessException;

/**
 * 用户上下文（ThreadLocal）
 * 存储当前请求的用户信息，避免重复查询
 */
public class UserContext {
    
    private static final ThreadLocal<UserInfo> CONTEXT = new ThreadLocal<>();
    
    public static void set(Long userId, Long tenantId, Integer roleLevel) {
        CONTEXT.set(new UserInfo(userId, tenantId, roleLevel));
    }
    
    public static Long getUserId() {
        UserInfo info = CONTEXT.get();
        return info != null ? info.getUserId() : null;
    }
    
    public static Long getTenantId() {
        UserInfo info = CONTEXT.get();
        return info != null ? info.getTenantId() : null;
    }
    
    public static Integer getRoleLevel() {
        UserInfo info = CONTEXT.get();
        return info != null ? info.getRoleLevel() : null;
    }
    
    public static void clear() {
        CONTEXT.remove();
    }
    
    /**
     * 水平越权校验：确保操作的是当前租户的数据
     * 运营商（tenant_id=0）可跨租户操作，无需校验
     * 
     * @param dataTenantId 数据所属的租户 ID
     * @throws BusinessException 如果不是运营商且租户 ID 不匹配
     */
    public static void validateHorizontalPermission(Long dataTenantId) {
        Long currentTenantId = getTenantId();
        
        // 1. 运营商（tenant_id=0）拥有所有权限，跳过校验
        if (currentTenantId != null && currentTenantId.equals(LevelCode.ROLE_LEVEL_TENANT_ID)) {
            return;
        }
        
        // 2. 匿名用户或未登录状态：允许访问公开接口产生的数据（如 chat.html）
        // 只要数据本身的 tenantId 是合法的，就允许操作，不进行越权拦截
        if (currentTenantId == null) {
            return;
        }
        
        // 3. 普通用户：必须属于同一租户
        if (!currentTenantId.equals(dataTenantId)) {
            throw new BusinessException(ResultCode.PERMISSION_DENIED);
        }
    }
    
    /**
     * 用户信息内部类
     */
    public static class UserInfo {
        private Long userId;
        private Long tenantId;
        private Integer roleLevel;
        
        public UserInfo(Long userId, Long tenantId, Integer roleLevel) {
            this.userId = userId;
            this.tenantId = tenantId;
            this.roleLevel = roleLevel;
        }
        
        public Long getUserId() { return userId; }
        public Long getTenantId() { return tenantId; }
        public Integer getRoleLevel() { return roleLevel; }
    }
}
