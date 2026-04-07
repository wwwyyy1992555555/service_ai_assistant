 package com.myproject.service_ai_assistant.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 咨询反馈表 - 实体类
 */
@Data
@TableName("consultation_feedback")
public class ConsultationFeedback implements Serializable {

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
     * 咨询记录 ID
     */
    private Long consultationId;

    /**
     * 会话 ID
     */
    private String sessionId;

    /**
     * 用户 ID
     */
    private String userId;

    /**
     * 满意度评分：1-5 星
     */
    private Integer satisfaction;

    /**
     * 反馈原因（JSON 数组）
     */
    private String feedbackReasons;

    /**
     * 用户建议
     */
    private String userSuggestion;

    /**
     * 是否已处理
     */
    private Integer isProcessed;

    /**
     * 处理备注
     */
    private String processRemark;

    /**
     * 处理人
     */
    private String processor;

    /**
     * 处理时间
     */
    private LocalDateTime processTime;

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

    /**
     * 用户姓名（非数据库字段，用于关联查询）
     */
    @TableField(exist = false)
    private String userName;

    /**
     * 用户手机号（非数据库字段，用于关联查询）
     */
    @TableField(exist = false)
    private String userPhone;
}
