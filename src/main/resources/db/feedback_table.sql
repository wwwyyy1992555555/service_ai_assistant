-- 咨询反馈表
CREATE TABLE IF NOT EXISTS consultation_feedback (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT 'ID',
    tenant_id BIGINT NOT NULL COMMENT '租户 ID',
    consultation_id BIGINT NOT NULL COMMENT '咨询记录 ID',
    session_id VARCHAR(100) COMMENT '会话 ID',
    user_id VARCHAR(50) COMMENT '用户 ID',
    satisfaction INT NOT NULL COMMENT '满意度评分：1-5 星',
    feedback_reasons TEXT COMMENT '反馈原因（JSON 数组）',
    user_suggestion TEXT COMMENT '用户建议',
    is_processed INT DEFAULT 0 COMMENT '是否已处理：0-未处理 1-已处理',
    process_remark TEXT COMMENT '处理备注',
    processor VARCHAR(50) COMMENT '处理人',
    process_time DATETIME COMMENT '处理时间',
    created_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    deleted INT DEFAULT 0 COMMENT '逻辑删除',
    INDEX idx_tenant (tenant_id),
    INDEX idx_consultation (consultation_id),
    INDEX idx_processed (is_processed),
    INDEX idx_created (created_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='咨询反馈表';
