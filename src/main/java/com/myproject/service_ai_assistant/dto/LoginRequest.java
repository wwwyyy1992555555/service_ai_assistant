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
     * 租户编码（普通租户登录时填写）
     */
    private String tenantCode;

    /**
     * 租户ID（超级管理员登录时传0，普通租户可不传由tenantCode解析）
     */
    private Long tenantId;
}
