package com.myproject.service_ai_assistant.dto;

import lombok.Data;

/**
 * 租户配置 DTO
 */
@Data
public class TenantConfigDTO {

    /**
     * 企业 Logo
     */
    private String logoUrl;

    /**
     * 租户名称（来自 tenant_info）
     */
    private String tenantName;

    /**
     * 欢迎语
     */
    private String welcomeMessage;

    /**
     * 主题颜色
     */
    private String themeColor;

    /**
     * 客服邮箱
     */
    private String serviceEmail;

    /**
     * 客服电话
     */
    private String servicePhone;

    /**
     * 工作时间描述
     */
    private String serviceTime;
}
