package com.myproject.service_ai_assistant.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * 创建用户请求
 */
public class UserCreateRequest {
    
    /**
     * 租户 ID（普通管理员必填，超级管理员可选）
     */
    private Long tenantId;
    
    /**
     * 租户编码（仅超级管理员使用，用于跨租户创建用户）
     */
    private String tenantCode;
    
    /**
     * 用户名
     */
    @NotBlank(message = "用户名不能为空")
    @Size(min = 3, max = 50, message = "用户名长度应在 3-50 个字符之间")
    private String username;
    
    /**
     * 真实姓名（可选）
     */
    @Size(min = 2, max = 50, message = "真实姓名长度应在 2-50 个字符之间")
    private String realName;
    
    /**
     * 密码
     */
    @NotBlank(message = "密码不能为空")
    @Size(min = 6, message = "密码长度至少为 6 位")
    private String password;
    
    /**
     * 手机号（可选）
     */
    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确")
    private String phone;
    
    /**
     * 邮箱（可选）
     */
    @Pattern(regexp = "^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$", message = "邮箱格式不正确")
    private String email;
    
    /**
     * 角色级别：0-超级管理员/1-普通管理员/2-操作员
     */
    @NotNull(message = "角色级别不能为空")
    private Integer roleLevel; // 0-超级管理员, 1-普通管理员, 2-操作员
    
    /**
     * 状态（默认启用）
     */
    private Integer status = 1; // 1-启用, 0-禁用

    // getter 和 setter 方法
    public Long getTenantId() {
        return tenantId;
    }

    public void setTenantId(Long tenantId) {
        this.tenantId = tenantId;
    }

    public String getTenantCode() {
        return tenantCode;
    }

    public void setTenantCode(String tenantCode) {
        this.tenantCode = tenantCode;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getRealName() {
        return realName;
    }

    public void setRealName(String realName) {
        this.realName = realName;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public Integer getRoleLevel() {
        return roleLevel;
    }

    public void setRoleLevel(Integer roleLevel) {
        this.roleLevel = roleLevel;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }
}