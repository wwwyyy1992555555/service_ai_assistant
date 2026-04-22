package com.myproject.service_ai_assistant.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

/**
 * 租户注册请求 DTO
 */
@Data
public class TenantRegisterRequest {

    /**
     * 企业名称
     */
    @NotBlank(message = "企业名称不能为空")
    private String tenantName;

    /**
     * 联系人姓名
     */
    @NotBlank(message = "联系人不能为空")
    private String contactPerson;

    /**
     * 联系电话
     */
    @NotBlank(message = "联系电话不能为空")
    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确")
    private String contactPhone;

    /**
     * 联系邮箱
     */
    @NotBlank(message = "邮箱不能为空")
    @Email(message = "邮箱格式不正确")
    private String contactEmail;

    /**
     * 邮箱验证码
     */
    @NotBlank(message = "请输入验证码")
    private String verifyCode;

    /**
     * 管理员用户名（登录账号）
     */
    @NotBlank(message = "管理员用户名不能为空")
    private String adminUsername;

    /**
     * 管理员密码
     */
    @NotBlank(message = "密码不能为空")
    private String adminPassword;
}
