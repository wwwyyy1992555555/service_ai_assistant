-- =============================================
-- 对话记录搜索优化索引
-- 创建时间：2026-03-27
-- 说明：为对话记录表添加搜索优化索引
-- =============================================

-- 1. 问题标题索引（前缀索引，节省空间）
CREATE INDEX idx_question ON consultation_record(question(100));

-- 2. 用户手机号索引（精确匹配）
CREATE INDEX idx_user_phone ON consultation_record(user_phone(20));

-- 3. 用户姓名索引（模糊匹配）
CREATE INDEX idx_user_name ON consultation_record(user_name(50));

-- 4. 会话 ID + 时间索引（会话分组查询）
CREATE INDEX idx_session_created ON consultation_record(session_id, created_time);

-- 5. 租户 ID + 时间索引（多租户隔离查询）
CREATE INDEX idx_tenant_created ON consultation_record(tenant_id, created_time);

-- 6. 创建时间索引（定期清理任务使用）
CREATE INDEX idx_created_time ON consultation_record(created_time);

-- 查看索引创建情况
SHOW INDEX FROM consultation_record;
