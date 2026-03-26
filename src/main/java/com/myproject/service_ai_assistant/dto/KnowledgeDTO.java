package com.myproject.service_ai_assistant.dto;

import lombok.Data;

/**
 * 知识 DTO
 */
@Data
public class KnowledgeDTO {
    
    private Long id;
    
    private Long tenantId;
    
    private Long categoryId;
    
    private String title;
    
    private String keywords;
    
    private String question;
    
    private String answer;
    
    private String contentType;
    
    private String attachments;
    
    private Integer publishStatus;
    
    private Integer isTop;
    
    private String author;
}
