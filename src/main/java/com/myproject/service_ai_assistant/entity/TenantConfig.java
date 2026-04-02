package com.myproject.service_ai_assistant.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 租户系统配置表 - 实体类 (固定字段设计)
 */
@Data
@TableName("tenant_config")
public class TenantConfig implements Serializable {

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
     * 企业 Logo
     */
    private String logoUrl;

    /**
     * 主题颜色
     */
    private String themeColor;

    /**
     * 欢迎语
     */
    private String welcomeMessage;

    /**
     * 企业名称
     */
    private String companyName;

    /**
     * 客服邮箱
     */
    private String serviceEmail;

    /**
     * 客服电话
     */
    private String servicePhone;

    /**
     * 工作时间
     */
    private String serviceTime;

    /**
     * 创建时间
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdTime;

    /**
     * 更新时间
     */
    @TableField(fill = FieldFill.UPDATE)
    private LocalDateTime updatedTime;

    /**
     * 逻辑删除：0-未删除 1-已删除
     */
    @TableLogic
    private Integer deleted;
}
