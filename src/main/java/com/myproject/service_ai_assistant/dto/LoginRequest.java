package com.myproject.service_ai_assistant.dto;

import lombok.Data;

import jakarta.validation.constraints.NotBlank;

/**
 * 登录请求 DTO
 */
@Data
public class LoginRequest {

    /**
     * 用户名
     */
    @NotBlank(message = "用户名不能为空")
    private String username;

    /**
     * 密码
     */
    @NotBlank(message = "密码不能为空")
    private String password;

    /**
     * 租户 ID
     */
    private Long tenantId;

    /**
     * 登录类型：tenant-租户登录/super-超级管理员登录
     */
    private String loginType = "tenant";
}
