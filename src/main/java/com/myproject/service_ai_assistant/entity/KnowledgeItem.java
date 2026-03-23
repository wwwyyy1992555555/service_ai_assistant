package com.myproject.service_ai_assistant.entity;

import com.baomidou.mybatisplus.annotation.*;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 知识条目表 - 实体类
 */
@Data
@TableName("knowledge_item")
@Schema(description = "知识条目信息")
public class KnowledgeItem implements Serializable {

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
     * 分类 ID
     */
    private Long categoryId;

    /**
     * 标题
     */
    private String title;

    /**
     * 关键词（多个用逗号分隔）
     */
    private String keywords;

    /**
     * 常见问题
     */
    private String question;

    /**
     * 标准答案（支持富文本）
     */
    private String answer;

    /**
     * 回答模板（参数化）
     */
    private String answerTemplate;

    /**
     * 内容类型：text-文本/video-视频/file-文件
     */
    private String contentType;

    /**
     * 附件列表（JSON 数组）
     */
    private String attachments;

    /**
     * 浏览次数
     */
    private Integer viewCount;

    /**
     * 有帮助次数
     */
    private Integer helpfulCount;

    /**
     * 是否置顶
     */
    private Integer isTop;

    /**
     * 是否热门
     */
    private Integer isHot;

    /**
     * 发布状态：0-草稿 1-已发布
     */
    private Integer publishStatus;

    /**
     * 作者
     */
    private String author;

    /**
     * 创建时间
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdTime;

    /**
     * 更新时间
     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedTime;

    /**
     * 逻辑删除
     */
    @TableLogic
    private Integer deleted;
}
