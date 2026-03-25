-- ========================================
-- 补充测试数据 - 用于数据看板展示
-- ========================================

-- 确保知识条目有足够的热门问题数据
-- 更新现有数据的浏览次数，让热门问题更"热"
UPDATE knowledge_item SET view_count = 2580 WHERE id = 1;
UPDATE knowledge_item SET view_count = 2156 WHERE id = 2;
UPDATE knowledge_item SET view_count = 1890 WHERE id = 3;
UPDATE knowledge_item SET view_count = 1654 WHERE id = 4;
UPDATE knowledge_item SET view_count = 1520 WHERE id = 5;
UPDATE knowledge_item SET view_count = 1380 WHERE id = 6;
UPDATE knowledge_item SET view_count = 1250 WHERE id = 7;
UPDATE knowledge_item SET view_count = 1120 WHERE id = 8;
UPDATE knowledge_item SET view_count = 980 WHERE id = 9;
UPDATE knowledge_item SET view_count = 850 WHERE id = 10;

-- 新增一些热门知识条目（如果现有的不够）
INSERT INTO `knowledge_item` (`tenant_id`, `category_id`, `title`, `question`, `keywords`, `answer`, `view_count`, `is_hot`, `publish_status`) VALUES
(1, 1, '一网通办服务平台使用说明', '什么是一网通办？怎么使用？', '一网通办，政务服务，在线办理', '一网通办是指通过一个统一的网上政务服务平台，实现各类政务服务事项的统一入口、统一受理、统一办理、统一反馈。用户可以通过电脑、手机 APP、微信小程序等多种渠道访问。', 3200, 1, 1),
(1, 1, '办事指南查询方法', '在哪里可以查看办事流程？', '办事指南，流程，查询', '您可以通过以下方式查看办事指南：1.登录政务服务网；2.关注官方微信公众号；3.拨打 12345 热线咨询；4.前往政务大厅现场咨询台索取指南。', 2890, 1, 1),
(1, 2, '医保电子凭证激活流程', '医保电子凭证怎么激活？', '医保，电子凭证，激活', '激活医保电子凭证有三种方式：1.通过国家医保服务平台 APP;2.通过微信或支付宝的医保电子凭证小程序；3.通过银行 APP。激活后即可在医院、药店扫码使用。', 2650, 1, 1),
(1, 3, '户口迁入政策咨询', '外地户口怎么迁入本市？', '户口，迁入，落户', '符合以下条件之一可申请落户：1.人才引进落户；2.积分落户；3.投靠落户 (夫妻、子女、父母);4.购房落户。具体条件请咨询公安机关户籍管理部门。', 2420, 1, 1),
(1, 4, '居住证积分管理办法', '居住证积分怎么计算？', '居住证，积分，管理', '居住证积分指标包括：年龄、学历、技能、社保缴纳年限、纳税贡献、创新创业等。积分可用于申请落户、子女入学、公租房等公共服务。', 2180, 1, 1);

-- 验证数据
SELECT '=== 热门问题 TOP10 ===' as info;
SELECT id, title, question, view_count, is_hot 
FROM knowledge_item 
WHERE tenant_id = 1 AND publish_status = 1 AND is_hot = 1 
ORDER BY view_count DESC 
LIMIT 10;

SELECT '=== 统计数据 ===' as info;
SELECT 
    (SELECT COUNT(*) FROM consultation_record WHERE tenant_id = 1 AND DATE(created_time) = CURDATE()) as today_consultations,
    (SELECT COUNT(*) FROM knowledge_item WHERE tenant_id = 1 AND publish_status = 1) as published_knowledge,
    (SELECT COUNT(DISTINCT category_id) FROM knowledge_item WHERE tenant_id = 1 AND publish_status = 1) as categories;

-- ========================================
-- 生成模拟对话记录数据（多轮对话）
-- ========================================

-- 会话 1：张三咨询社保问题（3 轮对话）
INSERT INTO `consultation_record` (`tenant_id`, `session_id`, `user_id`, `user_name`, `user_phone`, `question`, `answer`, `matched_knowledge_id`, `match_score`, `satisfaction`, `is_solved`, `need_human`, `device_type`, `ip_address`, `created_time`, `deleted`) VALUES
(1, 'session_001', 'user_001', '张三', '13800138001', '请问灵活就业人员怎么缴纳社保？', '灵活就业人员可以通过以下方式缴纳社保：1.到当地社保经办机构办理参保登记；2.通过政务服务网在线办理；3.使用手机 APP 自助缴费。缴费基数可在上下限之间自主选择。', 1, 0.95, 5, 1, 0, 'web', '192.168.1.100', DATE_SUB(NOW(), INTERVAL 1 HOUR), 0),
(1, 'session_001', 'user_001', '张三', '13800138001', '缴费基数是多少？', '2024 年灵活就业人员社保缴费基数下限为 3500 元，上限为 18000 元。您可以根据实际收入情况选择合适的基数。', NULL, 0.75, 5, 1, 0, 'web', '192.168.1.100', TIMESTAMPADD(MINUTE, -55, NOW()), 0),
(1, 'session_001', 'user_001', '张三', '13800138001', '好的，谢谢！', '不客气！如有其他问题，欢迎随时咨询。祝您生活愉快！', NULL, 0.0, 5, 1, 0, 'web', '192.168.1.100', TIMESTAMPADD(MINUTE, -50, NOW()), 0);

-- 会话 2：李四咨询公积金（4 轮对话）
INSERT INTO `consultation_record` (`tenant_id`, `session_id`, `user_id`, `user_name`, `user_phone`, `question`, `answer`, `matched_knowledge_id`, `match_score`, `satisfaction`, `is_solved`, `need_human`, `device_type`, `ip_address`, `created_time`, `deleted`) VALUES
(1, 'session_002', 'user_002', '李四', '13800138002', '公积金贷款额度怎么计算？', '公积金贷款额度计算公式：账户余额×缴存时间系数×倍数。具体倍数根据房屋类型确定，首套房最高 80 万，二套房最高 60 万。', 5, 0.92, 4, 1, 0, 'mobile', '192.168.1.101', DATE_SUB(NOW(), INTERVAL 2 HOUR), 0),
(1, 'session_002', 'user_002', '李四', '13800138002', '我账户有 5 万元，能贷多少？', '根据您的账户余额 5 万元，假设缴存时间系数为 1.2，倍数为 15 倍，理论可贷额度为：5 万×1.2×15=90 万。但最高不超过 80 万限额，所以您最多可贷 80 万。', NULL, 0.80, 4, 1, 0, 'mobile', '192.168.1.101', TIMESTAMPADD(MINUTE, -110, NOW()), 0),
(1, 'session_002', 'user_002', '李四', '13800138002', '利率是多少？', '公积金贷款利率：首套房 5 年以下 2.75%，5 年以上 3.25%；二套房上浮 10%。', NULL, 0.70, 4, 1, 0, 'mobile', '192.168.1.101', TIMESTAMPADD(MINUTE, -100, NOW()), 0),
(1, 'session_002', 'user_002', '李四', '13800138002', '非常感谢！', '不客气的！还有其他问题吗？', NULL, 0.0, 5, 1, 0, 'mobile', '192.168.1.101', TIMESTAMPADD(MINUTE, -95, NOW()), 0);

-- 会话 3：王五咨询居住证（2 轮对话）
INSERT INTO `consultation_record` (`tenant_id`, `session_id`, `user_id`, `user_name`, `user_phone`, `question`, `answer`, `matched_knowledge_id`, `match_score`, `satisfaction`, `is_solved`, `need_human`, `device_type`, `ip_address`, `created_time`, `deleted`) VALUES
(1, 'session_003', 'user_003', '王五', '13800138003', '居住证怎么办理？需要什么材料？', '办理居住证需要提供：1.身份证原件及复印件；2.居住地址证明（租房合同或房产证）；3.近期一寸照片 2 张。可到居住地派出所或社区警务室办理。', 4, 0.93, 5, 1, 0, 'web', '192.168.1.102', DATE_SUB(NOW(), INTERVAL 3 HOUR), 0),
(1, 'session_003', 'user_003', '王五', '13800138003', '多久能办好？', '居住证自受理之日起 15 个工作日内办结，可选择自取或邮寄。', NULL, 0.85, 5, 1, 0, 'web', '192.168.1.102', TIMESTAMPADD(MINUTE, -170, NOW()), 0);

-- 会话 4：赵六未匹配到知识的对话（3 轮对话）
INSERT INTO `consultation_record` (`tenant_id`, `session_id`, `user_id`, `user_name`, `user_phone`, `question`, `answer`, `matched_knowledge_id`, `match_score`, `satisfaction`, `is_solved`, `need_human`, `device_type`, `ip_address`, `created_time`, `deleted`) VALUES
(1, 'session_004', 'user_004', '赵六', '13800138004', '我想问一下关于创业补贴的事情', '抱歉，我暂时没有找到相关信息。您可以留下联系方式，我们会尽快安排专人为您解答。', NULL, 0.0, 2, 0, 1, 'mobile', '192.168.1.103', DATE_SUB(NOW(), INTERVAL 4 HOUR), 0),
(1, 'session_004', 'user_004', '赵六', '13800138004', '那转人工客服', '好的，已为您转接人工客服，请稍候...', NULL, 0.0, 2, 0, 1, 'mobile', '192.168.1.103', TIMESTAMPADD(MINUTE, -235, NOW()), 0),
(1, 'session_004', 'user_004', '赵六', '13800138004', '算了，我自己查吧', '非常抱歉没能帮到您。如需帮助可随时联系我们，祝您顺利！', NULL, 0.0, 3, 0, 0, 'mobile', '192.168.1.103', TIMESTAMPADD(MINUTE, -230, NOW()), 0);

-- 会话 5：匿名用户咨询（1 轮对话）
INSERT INTO `consultation_record` (`tenant_id`, `session_id`, `user_id`, `user_name`, `user_phone`, `question`, `answer`, `matched_knowledge_id`, `match_score`, `satisfaction`, `is_solved`, `need_human`, `device_type`, `ip_address`, `created_time`, `deleted`) VALUES
(1, 'session_005', NULL, NULL, NULL, '个人所得税 APP 密码忘了怎么办？', '个人所得税 APP 密码忘记可以通过以下方式找回：1.通过 APP 的“找回密码”功能，验证身份信息后重置；2.携带身份证到办税服务厅重置密码。', 2, 0.88, 4, 1, 0, 'web', '192.168.1.104', DATE_SUB(NOW(), INTERVAL 5 HOUR), 0);

-- 会话 6：多轮深入咨询（5 轮对话）
INSERT INTO `consultation_record` (`tenant_id`, `session_id`, `user_id`, `user_name`, `user_phone`, `question`, `answer`, `matched_knowledge_id`, `match_score`, `satisfaction`, `is_solved`, `need_human`, `device_type`, `ip_address`, `created_time`, `deleted`) VALUES
(1, 'session_006', 'user_006', '孙七', '13800138006', '新生儿医保怎么办理？', '新生儿医保办理需要：1.出生后 90 天内办理；2.携带户口本、出生证明、父母身份证；3.到户籍所在地街道社保所办理。', 3, 0.94, 5, 1, 0, 'web', '192.168.1.105', DATE_SUB(NOW(), INTERVAL 6 HOUR), 0),
(1, 'session_006', 'user_006', '孙七', '13800138006', '费用是多少？', '新生儿医保费用：出生当年免费，次年起按年度缴费，2024 年标准为每人每年 380 元。', NULL, 0.82, 5, 1, 0, 'web', '192.168.1.105', TIMESTAMPADD(MINUTE, -350, NOW()), 0),
(1, 'session_006', 'user_006', '孙七', '13800138006', '报销比例呢？', '新生儿医保报销比例：门诊 50%，住院 70%-90%（根据医院级别），大病保险另有额外报销。', NULL, 0.78, 5, 1, 0, 'web', '192.168.1.105', TIMESTAMPADD(MINUTE, -340, NOW()), 0),
(1, 'session_006', 'user_006', '孙七', '13800138006', '什么时候可以开始享受待遇？', '新生儿在出生后 90 天内办理参保并缴费的，从出生之日起享受医保待遇；超过 90 天办理的，从缴费次月起享受待遇。', NULL, 0.85, 5, 1, 0, 'web', '192.168.1.105', TIMESTAMPADD(MINUTE, -330, NOW()), 0),
(1, 'session_006', 'user_006', '孙七', '13800138006', '太详细了，谢谢！', '应该的！祝宝宝健康成长！有其他问题随时咨询我们。', NULL, 0.0, 5, 1, 0, 'web', '192.168.1.105', TIMESTAMPADD(MINUTE, -325, NOW()), 0);

-- 会话 7：不满意评价（2 轮对话）
INSERT INTO `consultation_record` (`tenant_id`, `session_id`, `user_id`, `user_name`, `user_phone`, `question`, `answer`, `matched_knowledge_id`, `match_score`, `satisfaction`, `is_solved`, `need_human`, `device_type`, `ip_address`, `created_time`, `deleted`) VALUES
(1, 'session_007', 'user_007', '周八', '13800138007', '你们这个系统太难用了', '非常抱歉给您带来不好的体验。请问您遇到了什么具体问题？我会尽力帮助您解决。', NULL, 0.0, 1, 0, 1, 'mobile', '192.168.1.106', DATE_SUB(NOW(), INTERVAL 7 HOUR), 0),
(1, 'session_007', 'user_007', '周八', '13800138007', '找个功能都找不到', '理解您的心情。请问您想找什么功能？我可以帮您指引。或者您拨打技术支持电话：12345-9，会有专人指导您操作。', NULL, 0.0, 2, 0, 1, 'mobile', '192.168.1.106', TIMESTAMPADD(MINUTE, -415, NOW()), 0);

-- 会话 8：满意评价（1 轮对话）
INSERT INTO `consultation_record` (`tenant_id`, `session_id`, `user_id`, `user_name`, `user_phone`, `question`, `answer`, `matched_knowledge_id`, `match_score`, `satisfaction`, `is_solved`, `need_human`, `device_type`, `ip_address`, `created_time`, `deleted`) VALUES
(1, 'session_008', 'user_008', '吴九', '13800138008', '护照过期了怎么换证？', '护照换发需要：1.携带旧护照、身份证、户口本原件；2.到出入境管理大厅办理；3.拍照、填表、缴费 280 元；4.7 个工作日后领取新护照。也可选择邮寄服务。', 1, 0.96, 5, 1, 0, 'web', '192.168.1.107', DATE_SUB(NOW(), INTERVAL 8 HOUR), 0);

-- 验证数据
SELECT '=== 会话统计 ===' as info;
SELECT 
    session_id,
    COUNT(*) as dialogue_count,
    MAX(user_name) as user_name,
    AVG(satisfaction) as avg_satisfaction,
    SUM(CASE WHEN matched_knowledge_id IS NOT NULL THEN 1 ELSE 0 END) as matched_count
FROM consultation_record
WHERE tenant_id = 1
GROUP BY session_id
ORDER BY created_time DESC;

SELECT '=== 对话记录总览 ===' as info;
SELECT 
    id,
    session_id,
    user_name,
    LEFT(question, 20) as question_preview,
    matched_knowledge_id,
    satisfaction,
    is_solved,
    device_type,
    created_time
FROM consultation_record
WHERE tenant_id = 1
ORDER BY created_time DESC
LIMIT 20;
