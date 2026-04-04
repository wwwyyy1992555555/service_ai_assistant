package com.myproject.service_ai_assistant.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.Delegate;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 租户信息表 - 实体类
 */
@Data
@TableName("tenant_info")
public class TenantInfo implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 租户 ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 企业名称
     */
    private String tenantName;

    /**
     * 企业编码（唯一标识）
     */
    private String tenantCode;

    /**
     * 行业类型：legal-律所/medical-医院/government-政务/community-社区
     */
    private String industryType;

    /**
     * 联系人
     */
    private String contactPerson;

    /**
     * 联系电话
     */
    private String contactPhone;

    /**
     * 状态：0-禁用 1-启用
     */
    private Integer status;

    /**
     * 备注
     */
    private String remark;

    /**
     * 创建时间
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdTime;

    /**
     * 逻辑删除：0-未删除 1-已删除
     */
    @TableLogic
    private Integer deleted;
}
