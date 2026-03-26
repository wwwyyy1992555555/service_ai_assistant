package com.myproject.service_ai_assistant.dto;

import lombok.Data;
import jakarta.validation.constraints.NotBlank;

/**
 * 用户信息解析请求
 */
@Data
public class UserInfoParseRequest {

    /**
     * 用户输入文本
     */
    @NotBlank(message = "输入文本不能为空")
    private String text;

    /**
     * 会话 ID
     */
    private String sessionId;

    /**
     * 租户 ID
     */
    private Long tenantId = 1L;
}
