package com.myproject.service_ai_assistant.dto;

import lombok.Data;

/**
 * 用户信息 DTO - 用于登录和前端展示
 */
@Data
public class UserDTO {

    /**
     * 用户 ID
     */
    private Long id;

    /**
     * 用户名
     */
    private String username;

    /**
     * 真实姓名
     */
    private String realName;

    /**
     * 手机号
     */
    private String phone;

    /**
     * 邮箱
     */
    private String email;

    /**
     * 角色：admin-管理员/operator-操作员
     */
    private String role;

    /**
     * 头像 URL
     */
    private String avatarUrl;

    /**
     * Token
     */
    private String token;

    /**
     * 密码（仅用于传输，不存储）
     */
    private transient String password;

    /**
     * 租户 ID
     */
    private Long tenantId;

    /**
     * 租户名称
     */
    private String tenantName;

    /**
     * 租户 Logo URL
     */
    private String tenantLogoUrl;

    /**
     * 租户主题色
     */
    private String tenantThemeColor;

    /**
     * 租户欢迎语
     */
    private String tenantWelcomeMessage;
}
