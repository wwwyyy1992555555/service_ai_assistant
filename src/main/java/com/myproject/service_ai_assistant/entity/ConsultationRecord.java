package com.myproject.service_ai_assistant.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 咨询对话记录表 - 实体类
 */
@Data
@TableName("consultation_record")
public class ConsultationRecord implements Serializable {

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
     * 会话 ID
     */
    private String sessionId;

    /**
     * 用户 ID（可匿名）
     */
    private String userId;

    /**
     * 用户昵称
     */
    private String userName;

    /**
     * 用户电话
     */
    private String userPhone;

    /**
     * 用户问题
     */
    private String question;

    /**
     * AI 回答
     */
    private String answer;

    /**
     * 匹配的知识 ID
     */
    private Long matchedKnowledgeId;

    /**
     * 匹配度分数
     */
    private Double matchScore;

    /**
     * 满意度：1-非常不满意 5-非常满意
     */
    private Integer satisfaction;

    /**
     * 用户反馈
     */
    private String feedback;

    /**
     * 是否解决
     */
    private Integer isSolved;

    /**
     * 是否需要转人工
     */
    private Integer needHuman;

    /**
     * IP 地址
     */
    private String ipAddress;

    /**
     * 设备类型
     */
    private String deviceType;

    /**
     * 创建时间
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdTime;

    /**
     * 逻辑删除
     */
    @TableLogic
    private Integer deleted;
}
