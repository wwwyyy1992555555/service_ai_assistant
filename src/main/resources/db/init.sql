-- ========================================
-- AI 智库企业咨询平台 - 数据库初始化脚本
-- ========================================

-- 创建数据库
CREATE DATABASE IF NOT EXISTS ai_think_tank DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE ai_think_tank;

-- ========================================
-- 1. 租户表（多企业隔离）
-- ========================================
DROP TABLE IF EXISTS `tenant_info`;
CREATE TABLE `tenant_info` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '租户 ID',
  `tenant_name` VARCHAR(100) NOT NULL COMMENT '企业名称',
  `tenant_code` VARCHAR(50) NOT NULL COMMENT '企业编码（唯一标识）',
  `industry_type` VARCHAR(50) NOT NULL COMMENT '行业类型：legal-律所/medical-医院/government-政务/community-社区',
  `contact_person` VARCHAR(50) DEFAULT NULL COMMENT '联系人',
  `contact_phone` VARCHAR(20) DEFAULT NULL COMMENT '联系电话',
  `logo_url` VARCHAR(500) DEFAULT NULL COMMENT '企业 Logo',
  `theme_color` VARCHAR(20) DEFAULT '#1890ff' COMMENT '主题色',
  `welcome_message` VARCHAR(500) DEFAULT '您好，请问有什么可以帮您？' COMMENT '欢迎语',
  `status` TINYINT DEFAULT 1 COMMENT '状态：0-禁用 1-启用',
  `expire_time` DATETIME DEFAULT NULL COMMENT '过期时间（NULL 表示永久有效）',
  `remark` VARCHAR(1000) DEFAULT NULL COMMENT '备注',
  `created_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `deleted` TINYINT DEFAULT 0 COMMENT '逻辑删除：0-未删除 1-已删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_tenant_code` (`tenant_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='租户信息表';

-- ========================================
-- 2. 知识库分类表（多级分类）
-- ========================================
DROP TABLE IF EXISTS `knowledge_category`;
CREATE TABLE `knowledge_category` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `tenant_id` BIGINT NOT NULL COMMENT '租户 ID',
  `category_name` VARCHAR(100) NOT NULL COMMENT '分类名称',
  `parent_id` BIGINT DEFAULT 0 COMMENT '父级 ID（0 为顶级）',
  `level` INT DEFAULT 1 COMMENT '层级',
  `category_code` VARCHAR(50) DEFAULT NULL COMMENT '分类编码',
  `icon` VARCHAR(100) DEFAULT NULL COMMENT '图标',
  `sort_order` INT DEFAULT 0 COMMENT '排序',
  `status` TINYINT DEFAULT 1 COMMENT '状态',
  `created_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `deleted` TINYINT DEFAULT 0,
  PRIMARY KEY (`id`),
  KEY `idx_tenant_parent` (`tenant_id`, `parent_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='知识库分类表';

-- ========================================
-- 3. 知识条目表（核心知识库）
-- ========================================
DROP TABLE IF EXISTS `knowledge_item`;
CREATE TABLE `knowledge_item` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `tenant_id` BIGINT NOT NULL COMMENT '租户 ID',
  `category_id` BIGINT DEFAULT NULL COMMENT '分类 ID',
  `title` VARCHAR(200) NOT NULL COMMENT '标题',
  `keywords` VARCHAR(500) DEFAULT NULL COMMENT '关键词（多个用逗号分隔）',
  `question` VARCHAR(500) DEFAULT NULL COMMENT '常见问题',
  `answer` TEXT NOT NULL COMMENT '标准答案（支持富文本）',
  `answer_template` TEXT COMMENT '回答模板（参数化）',
  `content_type` VARCHAR(20) DEFAULT 'text' COMMENT '内容类型：text-文本/video-视频/file-文件',
  `attachments` JSON DEFAULT NULL COMMENT '附件列表（JSON 数组）',
  `view_count` INT DEFAULT 0 COMMENT '浏览次数',
  `helpful_count` INT DEFAULT 0 COMMENT '有帮助次数',
  `is_top` TINYINT DEFAULT 0 COMMENT '是否置顶',
  `is_hot` TINYINT DEFAULT 0 COMMENT '是否热门',
  `publish_status` TINYINT DEFAULT 1 COMMENT '发布状态：0-草稿 1-已发布',
  `author` VARCHAR(50) DEFAULT NULL COMMENT '作者',
  `created_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `updated_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` TINYINT DEFAULT 0,
  PRIMARY KEY (`id`),
  KEY `idx_tenant_category` (`tenant_id`, `category_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='知识条目表';

-- ========================================
-- 4. 对话记录表
-- ========================================
DROP TABLE IF EXISTS `consultation_record`;
CREATE TABLE `consultation_record` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `tenant_id` BIGINT NOT NULL COMMENT '租户 ID',
  `session_id` VARCHAR(100) NOT NULL COMMENT '会话 ID',
  `user_id` VARCHAR(100) DEFAULT NULL COMMENT '用户 ID（可匿名）',
  `user_name` VARCHAR(100) DEFAULT NULL COMMENT '用户昵称',
  `user_phone` VARCHAR(20) DEFAULT NULL COMMENT '用户电话',
  `question` TEXT NOT NULL COMMENT '用户问题',
  `answer` TEXT COMMENT 'AI 回答',
  `matched_knowledge_id` BIGINT DEFAULT NULL COMMENT '匹配的知识 ID',
  `match_score` DECIMAL(3,2) DEFAULT NULL COMMENT '匹配度分数（0-1）',
  `satisfaction` TINYINT DEFAULT NULL COMMENT '满意度：1-非常不满意 5-非常满意',
  `feedback` VARCHAR(500) DEFAULT NULL COMMENT '用户反馈',
  `is_solved` TINYINT DEFAULT 1 COMMENT '是否解决',
  `need_human` TINYINT DEFAULT 0 COMMENT '是否需要转人工',
  `ip_address` VARCHAR(50) DEFAULT NULL COMMENT 'IP 地址',
  `device_type` VARCHAR(50) DEFAULT 'web' COMMENT '设备类型',
  `created_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `deleted` TINYINT DEFAULT 0,
  PRIMARY KEY (`id`),
  KEY `idx_tenant_session` (`tenant_id`, `session_id`),
  KEY `idx_session_tenant_time` (`session_id`, `tenant_id`, `created_time`),
  KEY `idx_tenant_time` (`tenant_id`, `created_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='咨询对话记录表';

-- ========================================
-- 5. 租户配置表
-- ========================================
DROP TABLE IF EXISTS `tenant_config`;
CREATE TABLE `tenant_config` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT 'ID',
  `tenant_id` BIGINT NOT NULL COMMENT '租户 ID',
  `logo_url` VARCHAR(500) DEFAULT NULL COMMENT '企业 Logo',
  `theme_color` VARCHAR(20) DEFAULT '#1890ff' COMMENT '主题颜色',
  `welcome_message` VARCHAR(500) DEFAULT '您好，请问有什么可以帮您？' COMMENT '欢迎语',
  `company_name` VARCHAR(100) DEFAULT NULL COMMENT '企业名称',
  `service_email` VARCHAR(100) DEFAULT NULL COMMENT '客服邮箱',
  `service_phone` VARCHAR(20) DEFAULT NULL COMMENT '客服电话',
  `service_time` VARCHAR(200) DEFAULT NULL COMMENT '工作时间',
  `created_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` TINYINT DEFAULT 0 COMMENT '逻辑删除:0-未删除 1-已删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_tenant_id` (`tenant_id`),
  KEY `idx_tenant_id` (`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='租户系统配置表';

-- ========================================
-- 6. 用户信息表
-- ========================================
DROP TABLE IF EXISTS `user_info`;
CREATE TABLE `user_info` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `tenant_id` BIGINT NOT NULL COMMENT '租户 ID',
  `username` VARCHAR(50) NOT NULL COMMENT '用户名（登录账号）',
  `password` VARCHAR(100) NOT NULL COMMENT '密码（加密存储）',
  `real_name` VARCHAR(50) DEFAULT NULL COMMENT '真实姓名',
  `phone` VARCHAR(20) DEFAULT NULL COMMENT '手机号',
  `email` VARCHAR(100) DEFAULT NULL COMMENT '邮箱',
  `role_level` INT DEFAULT 1 COMMENT '角色级别：0-超级管理员/1-普通管理员/2-操作员',
  `status` TINYINT DEFAULT 1 COMMENT '状态：0-禁用 1-启用',
  `avatar_url` VARCHAR(500) DEFAULT NULL COMMENT '头像 URL',
  `last_login_time` DATETIME DEFAULT NULL COMMENT '最后登录时间',
  `last_login_ip` VARCHAR(50) DEFAULT NULL COMMENT '最后登录 IP',
  `created_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `updated_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` TINYINT DEFAULT 0,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_tenant_username` (`tenant_id`, `username`, `deleted`),
  KEY `idx_tenant_id` (`tenant_id`),
  KEY `idx_phone` (`phone`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户信息表';

-- ========================================
-- 7. 咨询反馈表
-- ========================================
DROP TABLE IF EXISTS `consultation_feedback`;
CREATE TABLE `consultation_feedback` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `tenant_id` BIGINT NOT NULL COMMENT '租户 ID',
  `consultation_id` BIGINT NOT NULL COMMENT '咨询记录 ID',
  `session_id` VARCHAR(100) COMMENT '会话 ID',
  `user_id` VARCHAR(50) COMMENT '用户 ID',
  `satisfaction` INT NOT NULL COMMENT '满意度评分：1-5 星',
  `feedback_reasons` TEXT COMMENT '反馈原因（JSON 数组）',
  `user_suggestion` TEXT COMMENT '用户建议',
  `is_processed` INT DEFAULT 0 COMMENT '是否已处理：0-未处理 1-已处理',
  `process_remark` TEXT COMMENT '处理备注',
  `processor` VARCHAR(50) COMMENT '处理人',
  `process_time` DATETIME COMMENT '处理时间',
  `created_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `deleted` TINYINT DEFAULT 0 COMMENT '逻辑删除',
  PRIMARY KEY (`id`),
  KEY `idx_tenant_consultation` (`tenant_id`, `consultation_id`),
  KEY `idx_session` (`session_id`),
  KEY `idx_processed_time` (`is_processed`, `created_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='咨询反馈表';

-- ========================================
-- 插入示例数据（政务服务场景）
-- ========================================

-- 示例租户
INSERT INTO `tenant_info` (`tenant_name`, `tenant_code`, `industry_type`, `contact_person`, `contact_phone`, `theme_color`, `welcome_message`) VALUES
('XX 市政务服务中心', 'gov_demo_001', 'government', '张主任', '13800138000', '#1890ff', '您好，XX 市政务服务中心很高兴为您服务！');

-- 示例知识分类
INSERT INTO `knowledge_category` (`tenant_id`, `category_name`, `parent_id`, `level`) VALUES
(1, '办事服务', 0, 1),
(1, '社保医保', 0, 1),
(1, '户籍办理', 0, 1),
(1, '居住证办理', 0, 1),
(1, '公积金', 0, 1),
(1, '工商登记', 0, 1),
(1, '税务服务', 0, 1),
(1, '医疗卫生', 0, 1);

-- 示例知识条目（丰富的调试数据）
INSERT INTO `knowledge_item` (`tenant_id`, `category_id`, `title`, `question`, `keywords`, `answer`, `view_count`, `is_hot`, `publish_status`) VALUES
(1, 4, '居住证办理流程', '居住证怎么办理？', '居住证，办理，流程', '办理居住证需要携带本人身份证、租房合同或房产证、近期一寸照片到居住地派出所或政务服务中心公安窗口办理。也可通过"互联网 + 政务服务"平台在线申请。', 1256, 1, 1),
(1, 2, '社保卡补办流程', '社保卡丢了怎么补办？', '社保卡，补办，丢失', '社保卡丢失后，请先拨打 12333 服务热线挂失，然后携带身份证到社保卡服务网点办理书面挂失和补办手续。补办费用 20 元，7-15 个工作日可领取新卡。', 1089, 1, 1),
(1, 5, '公积金提取条件', '公积金提取条件是什么？', '公积金，提取，条件', '符合以下情形之一即可提取：1.购买、建造、翻建自住住房；2.偿还购房贷款本息；3.租赁住房；4.离退休；5.完全丧失劳动能力并与单位终止劳动关系；6.出境定居。', 967, 1, 1),
(1, 6, '营业执照办理流程', '营业执照怎么办？', '营业执照，办理，企业注册', '办理营业执照需准备：1.企业名称预先核准通知书；2.公司章程；3.股东身份证明；4.住所证明。可通过政务服务网在线申请或到工商窗口现场办理，3-5 个工作日可领取。', 856, 1, 1),
(1, 2, '新生儿医保办理', '新生儿医保如何办理？', '新生儿，医保，办理', '新生儿出生后 90 天内，由父母携带户口本、出生证明、父母身份证到户籍所在地社区或街道办理参保登记手续。缴费标准为每年 350 元，缴费后即可享受医保待遇。', 745, 1, 1),
(1, 3, '身份证补办流程', '身份证丢了怎么补办？', '身份证，补办，丢失', '身份证丢失后，请携带户口本到户籍地派出所或政务服务中心公安窗口办理挂失和补办手续。可选择邮寄方式领取新身份证，邮费到付。', 623, 0, 1),
(1, 4, '居住证续签办理', '居住证到期了怎么续签？', '居住证，续签，到期', '居住证有效期为 1 年，到期前 30 日内可申请续签。续签需携带本人身份证、居住证原件、居住地址证明材料到原办证点或就近派出所办理。', 534, 0, 1),
(1, 5, '公积金贷款申请', '公积金贷款怎么申请？', '公积金，贷款，申请', '申请公积金贷款需满足：连续缴存 6 个月以上、有稳定收入、信用良好、有购房合同。准备身份证、户口本、收入证明、购房合同等材料到公积金管理中心办理。', 489, 0, 1),
(1, 7, '个体工商户注册登记', '个体户营业执照怎么办？', '个体户，营业执照，注册', '办理个体户营业执照需提供：1.经营者身份证；2.经营场所证明；3.个体工商户名称预先核准通知书。可通过政务服务网全程电子化办理，1-3 个工作日即可下证。', 412, 0, 1),
(1, 8, '医院挂号预约', '怎么预约挂号看病？', '医院，挂号，预约', '可通过以下方式预约挂号：1.医院官方微信公众号；2.健康 XX APP;3.电话 114 预约；4.现场自助机。建议提前 1-7 天预约，就诊当天携带身份证或医保卡取号。', 378, 0, 1),
(1, 2, '失业保险金申领', '失业金怎么领取？', '失业金，失业保险，申领', '失业人员申领失业保险金需满足：失业前用人单位和本人已缴纳失业保险费满一年、非因本人意愿中断就业。携带身份证、解除劳动合同证明到公共就业服务机构办理。', 345, 0, 1),
(1, 7, '税务登记办理', '公司成立后怎么办理税务登记？', '税务登记，报税，公司注册', '公司成立后 30 日内需办理税务登记。携带营业执照副本、公章、法人身份证、财务负责人身份证到主管税务机关办理。现在多证合一，可在工商注册时一并办理。', 298, 0, 1),
(1, 3, '户口迁移办理', '户口迁移需要什么材料？', '户口迁移，落户，材料', '户口迁移需提供：1.迁入地公安机关出具的准予迁入证明；2.原户籍地派出所出具的户口迁移证；3.申请人身份证；4.相关证明材料 (如房产证、结婚证等)。', 267, 0, 1),
(1, 4, '暂住登记办理', '暂住证怎么办理？', '暂住证，流动人口，登记', '外来务工人员应在到达居住地 7 日内，携带身份证、租房合同或住宿证明、就业证明到居住地派出所或社区警务室办理暂住登记。', 234, 0, 1),
(1, 5, '租房公积金提取', '租房怎么提取公积金？', '公积金，租房，提取', '租房提取公积金需提供：1.身份证；2.婚姻状况证明；3.租房合同；4.租金发票。每年可提取一次，提取金额不超过当年房租支出。可通过政务服务网在线办理。', 212, 0, 1);

-- 示例对话记录（调试数据）
INSERT INTO `consultation_record` (`tenant_id`, `session_id`, `user_name`, `question`, `answer`, `matched_knowledge_id`, `match_score`, `is_solved`, `satisfaction`, `device_type`) VALUES
(1, 'session_001', '张先生', '居住证怎么办理？', '办理居住证需要携带本人身份证、租房合同或房产证、近期一寸照片到居住地派出所或政务服务中心公安窗口办理。也可通过"互联网 + 政务服务"平台在线申请。', 1, 0.95, 1, 5, 'web'),
(1, 'session_002', '李女士', '社保卡丢了怎么补办？', '社保卡丢失后，请先拨打 12333 服务热线挂失，然后携带身份证到社保卡服务网点办理书面挂失和补办手续。补办费用 20 元，7-15 个工作日可领取新卡。', 2, 0.92, 1, 4, 'web'),
(1, 'session_003', '王先生', '公积金提取条件是什么？', '符合以下情形之一即可提取：1.购买、建造、翻建自住住房；2.偿还购房贷款本息；3.租赁住房；4.离退休；5.完全丧失劳动能力并与单位终止劳动关系；6.出境定居。', 3, 0.89, 1, 5, 'mobile'),
(1, 'session_004', '赵女士', '营业执照怎么办？', '办理营业执照需准备：1.企业名称预先核准通知书；2.公司章程；3.股东身份证明；4.住所证明。可通过政务服务网在线申请或到工商窗口现场办理，3-5 个工作日可领取。', 4, 0.91, 1, 4, 'web'),
(1, 'session_005', '刘先生', '新生儿医保如何办理？', '新生儿出生后 90 天内，由父母携带户口本、出生证明、父母身份证到户籍所在地社区或街道办理参保登记手续。缴费标准为每年 350 元，缴费后即可享受医保待遇。', 5, 0.88, 1, 5, 'mobile'),
(1, 'session_006', '陈女士', '身份证丢了怎么补办？', '身份证丢失后，请携带户口本到户籍地派出所或政务服务中心公安窗口办理挂失和补办手续。可选择邮寄方式领取新身份证，邮费到付。', 6, 0.93, 1, 4, 'web'),
(1, 'session_007', '杨先生', '居住证到期了怎么续签？', '居住证有效期为 1 年，到期前 30 日内可申请续签。续签需携带本人身份证、居住证原件、居住地址证明材料到原办证点或就近派出所办理。', 7, 0.87, 1, 3, 'web'),
(1, 'session_008', '周女士', '公积金贷款怎么申请？', '申请公积金贷款需满足：连续缴存 6 个月以上、有稳定收入、信用良好、有购房合同。准备身份证、户口本、收入证明、购房合同等材料到公积金管理中心办理。', 8, 0.90, 1, 5, 'mobile'),
(1, 'session_009', '吴先生', '个体户营业执照怎么办？', '办理个体户营业执照需提供：1.经营者身份证；2.经营场所证明；3.个体工商户名称预先核准通知书。可通过政务服务网全程电子化办理，1-3 个工作日即可下证。', 9, 0.86, 1, 4, 'web'),
(1, 'session_010', '郑女士', '怎么预约挂号看病？', '可通过以下方式预约挂号：1.医院官方微信公众号；2.健康 XX APP;3.电话 114 预约；4.现场自助机。建议提前 1-7 天预约，就诊当天携带身份证或医保卡取号。', 10, 0.84, 1, 4, 'mobile'),
(1, 'session_011', '孙先生', '失业金怎么领取？', '失业人员申领失业保险金需满足：失业前用人单位和本人已缴纳失业保险费满一年、非因本人意愿中断就业。携带身份证、解除劳动合同证明到公共就业服务机构办理。', 11, 0.85, 1, 3, 'web'),
(1, 'session_012', '马女士', '公司成立后怎么办理税务登记？', '公司成立后 30 日内需办理税务登记。携带营业执照副本、公章、法人身份证、财务负责人身份证到主管税务机关办理。现在多证合一，可在工商注册时一并办理。', 12, 0.82, 1, 4, 'web'),
(1, 'session_013', '朱先生', '户口迁移需要什么材料？', '户口迁移需提供：1.迁入地公安机关出具的准予迁入证明；2.原户籍地派出所出具的户口迁移证；3.申请人身份证；4.相关证明材料 (如房产证、结婚证等)。', 13, 0.88, 1, 5, 'mobile'),
(1, 'session_014', '胡女士', '暂住证怎么办理？', '外来务工人员应在到达居住地 7 日内，携带身份证、租房合同或住宿证明、就业证明到居住地派出所或社区警务室办理暂住登记。', 14, 0.81, 1, 4, 'web'),
(1, 'session_015', '林先生', '租房怎么提取公积金？', '租房提取公积金需提供：1.身份证；2.婚姻状况证明；3.租房合同；4.租金发票。每年可提取一次，提取金额不超过当年房租支出。可通过政务服务网在线办理。', 15, 0.79, 1, 3, 'mobile'),
(1, 'session_016', '何先生', '这个网站不好用', '抱歉给您带来不便，请问具体遇到什么问题了呢？您可以详细描述一下，我们会安排专人为您解答和改进。', NULL, 0.0, 0, 2, 'web'),
(1, 'session_017', '罗女士', '什么时候下班？', '我们的服务时间是周一至周日 9:00-17:00,中午不休息。建议您合理安排时间前来办理业务。', NULL, 0.0, 1, 4, 'mobile'),
(1, 'session_018', '梁先生', '可以代办吗？', '大部分业务支持代办，代办人需携带本人身份证原件、委托人身份证复印件及授权委托书。具体业务要求请咨询相关窗口。', NULL, 0.0, 1, 4, 'web'),
(1, 'session_019', '宋女士', '周末上班吗？', '我中心周末正常上班，服务时间为 9:00-17:00,欢迎您前来办理业务。', NULL, 0.0, 1, 5, 'mobile'),
(1, 'session_020', '韩先生', '停车方便吗？', '市民之家地下停车场免费对外开放，车位充足。也可乘坐公共交通工具前往，地铁 1 号线市民之家站直达。', NULL, 0.0, 1, 5, 'web');

-- 租户配置初始化数据
INSERT INTO `tenant_config` (`tenant_id`, `logo_url`, `theme_color`, `welcome_message`, `company_name`, `service_email`, `service_phone`, `service_time`, `deleted`) VALUES
(1, '', '#1890ff', '您好，XX 市政务服务中心很高兴为您服务！', 'XX 市政务服务中心', 'service@gov.cn', '12345', '工作时间：周一至周日 9:00-17:00', 0)
ON DUPLICATE KEY UPDATE 
    `logo_url` = VALUES(`logo_url`),
    `theme_color` = VALUES(`theme_color`),
    `welcome_message` = VALUES(`welcome_message`),
    `company_name` = VALUES(`company_name`),
    `service_email` = VALUES(`service_email`),
    `service_phone` = VALUES(`service_phone`),
    `service_time` = VALUES(`service_time`);

-- 用户初始化数据（密码为 123456，使用 BCrypt 加密存储）
-- 加密方式：BCrypt.hashpw("123456", BCrypt.gensalt())
-- 注意：tenant_id=0 表示超级管理员，不属于任何租户，通过用户名区分不同的超级管理员
-- 安全说明：生产环境请修改默认密码，并启用密码强度校验
INSERT INTO `user_info` (`tenant_id`, `username`, `password`, `real_name`, `phone`, `email`, `role_level`, `status`) VALUES
(0, 'admin', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', '超级管理员', '13900139000', 'admin@platform.com', 0, 1),
(0, 'superadmin', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', '平台管理员', '13900139001', 'superadmin@platform.com', 0, 1),
(1, 'admin', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', '管理员', '13800138000', 'admin@gov.cn', 1, 1),
(1, 'operator', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', '操作员', '13800138001', 'operator@gov.cn', 2, 1);