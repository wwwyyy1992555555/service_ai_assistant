package com.myproject.service_ai_assistant.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 智能问答请求 DTO
 */
@Data
public class ConsultRequest {
    
    @NotBlank(message = "会话 ID 不能为空")
    private String sessionId;
    
    @NotBlank(message = "问题不能为空")
    private String question;
    
    private Long tenantId = 1L; // 默认租户
    
    private String userId;
    
    private String userName;
    
    private String userPhone;
    
    private String deviceType = "web";
    
    private String ipAddress;
}
