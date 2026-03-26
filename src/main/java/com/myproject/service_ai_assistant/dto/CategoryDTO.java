package com.myproject.service_ai_assistant.dto;

import lombok.Data;

/**
 * 分类 DTO
 */
@Data
public class CategoryDTO {
    
    private Long id;
    
    private Long tenantId;
    
    private String categoryName;
    
    private Integer sortOrder;
    
    private Integer status;
}
