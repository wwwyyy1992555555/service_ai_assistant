package com.myproject.service_ai_assistant.dto;

import lombok.Data;

/**
 * 密码强度 DTO
 */
@Data
public class PasswordStrengthDTO {

    /**
     * 强度分数 (0-100)
     */
    private int score;

    /**
     * 强度等级
     */
    private String level;

    /**
     * 强度等级标签
     */
    private String label;

    /**
     * 强度颜色
     */
    private String color;

    /**
     * 提示信息
     */
    private String message;
}
