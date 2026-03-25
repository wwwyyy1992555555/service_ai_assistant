package com.myproject.service_ai_assistant.entity;

import com.baomidou.mybatisplus.annotation.*;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 知识库分类表 - 实体类
 */
@Data
@TableName("knowledge_category")
@Schema(description = "知识库分类信息")
public class KnowledgeCategory implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 租户 ID
     */
    private Long tenantId;

    /**
     * 分类名称
     */
    private String categoryName;

    /**
     * 父级 ID（0 为顶级）
     */
    private Long parentId;

    /**
     * 层级
     */
    private Integer level;

    /**
     * 分类编码
     */
    private String categoryCode;

    /**
     * 图标
     */
    private String icon;

    /**
     * 排序
     */
    private Integer sortOrder;

    /**
     * 状态
     */
    private Integer status;

    /**
     * 创建时间
     */
    private LocalDateTime createdTime;

    /**
     * 删除标志
     */
    private Integer deleted;
}
