package com.myproject.service_ai_assistant.dto;

import lombok.Data;

import java.util.List;

/**
 * 智能问答响应 DTO
 */
@Data
public class ConsultResponse {
    
    private String answer;
    
    private Long matchedKnowledgeId;
    
    private Double matchScore;
    
    private String knowledgeTitle;
    
    private String categoryName;
    
    private Integer viewCount;
    
    private Long consultationId; // 咨询记录 ID
    
    private List<String> suggestedQuestions;
}
