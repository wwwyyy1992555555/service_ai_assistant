package com.myproject.service_ai_assistant.dto;

import lombok.Data;

/**
 * 系统配置 DTO
 */
@Data
public class SystemConfigDTO {

    /**
     * 企业名称
     */
    private String companyName;

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
    private String email;

    /**
     * 客服电话
     */
    private String phone;
}
