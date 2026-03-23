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
  `remark` VARCHAR(1000) DEFAULT NULL COMMENT '备注',
  `created_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `deleted` TINYINT DEFAULT 0 COMMENT '逻辑删除：0-未删除 1-已删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_tenant_code` (`tenant_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='租户信息表';

-- ========================================
-- 2. 企业基础信息表
-- ========================================
DROP TABLE IF EXISTS `company_profile`;
CREATE TABLE `company_profile` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `tenant_id` BIGINT NOT NULL COMMENT '租户 ID',
  `company_name` VARCHAR(200) NOT NULL COMMENT '企业全称',
  `company_short_name` VARCHAR(100) DEFAULT NULL COMMENT '企业简称',
  `founded_year` INT DEFAULT NULL COMMENT '成立年份',
  `company_size` VARCHAR(50) DEFAULT NULL COMMENT '企业规模：如"50-100 人"',
  `description` TEXT COMMENT '企业简介',
  `history` TEXT COMMENT '发展历程',
  `business_scope` TEXT COMMENT '业务范围',
  `honors` TEXT COMMENT '荣誉资质',
  `address` VARCHAR(500) DEFAULT NULL COMMENT '办公地址',
  `latitude` DECIMAL(10,6) DEFAULT NULL COMMENT '纬度',
  `longitude` DECIMAL(11,6) DEFAULT NULL COMMENT '经度',
  `working_hours` VARCHAR(100) DEFAULT '周一至周五 9:00-18:00' COMMENT '营业时间',
  `website` VARCHAR(200) DEFAULT NULL COMMENT '官方网站',
  `email` VARCHAR(100) DEFAULT NULL COMMENT '官方邮箱',
  `created_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `updated_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` TINYINT DEFAULT 0,
  PRIMARY KEY (`id`),
  KEY `idx_tenant_id` (`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='企业基础信息表';

-- ========================================
-- 3. 服务网点表（位置、导航）
-- ========================================
DROP TABLE IF EXISTS `service_location`;
CREATE TABLE `service_location` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `tenant_id` BIGINT NOT NULL COMMENT '租户 ID',
  `location_name` VARCHAR(100) NOT NULL COMMENT '网点名称',
  `location_type` VARCHAR(50) DEFAULT NULL COMMENT '网点类型：总部/分店/办事处',
  `province` VARCHAR(50) DEFAULT NULL COMMENT '省',
  `city` VARCHAR(50) DEFAULT NULL COMMENT '市',
  `district` VARCHAR(50) DEFAULT NULL COMMENT '区',
  `detail_address` VARCHAR(500) DEFAULT NULL COMMENT '详细地址',
  `latitude` DECIMAL(10,6) DEFAULT NULL COMMENT '纬度',
  `longitude` DECIMAL(11,6) DEFAULT NULL COMMENT '经度',
  `phone` VARCHAR(50) DEFAULT NULL COMMENT '联系电话',
  `opening_hours` VARCHAR(100) DEFAULT NULL COMMENT '营业时间',
  `facilities` VARCHAR(500) DEFAULT NULL COMMENT '设施：停车场/WiFi/无障碍通道',
  `traffic_guide` TEXT COMMENT '交通指引',
  `sort_order` INT DEFAULT 0 COMMENT '排序',
  `status` TINYINT DEFAULT 1 COMMENT '状态：0-关闭 1-营业中',
  `created_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `deleted` TINYINT DEFAULT 0,
  PRIMARY KEY (`id`),
  KEY `idx_tenant_id` (`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='服务网点表';

-- ========================================
-- 4. 知识库分类表（多级分类）
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
-- 5. 知识条目表（核心知识库）
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
-- 6. 专家/团队表
-- ========================================
DROP TABLE IF EXISTS `expert_team`;
CREATE TABLE `expert_team` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `tenant_id` BIGINT NOT NULL COMMENT '租户 ID',
  `expert_name` VARCHAR(100) NOT NULL COMMENT '专家姓名',
  `title` VARCHAR(100) DEFAULT NULL COMMENT '职称/职务',
  `specialty` VARCHAR(500) DEFAULT NULL COMMENT '擅长领域',
  `introduction` TEXT COMMENT '个人简介',
  `photo_url` VARCHAR(500) DEFAULT NULL COMMENT '照片',
  `years_experience` INT DEFAULT NULL COMMENT '从业年限',
  `success_rate` DECIMAL(5,2) DEFAULT NULL COMMENT '成功率（%）',
  `case_count` INT DEFAULT 0 COMMENT '案例数量',
  `schedule_info` JSON DEFAULT NULL COMMENT '排班信息（JSON）',
  `appointment_enabled` TINYINT DEFAULT 1 COMMENT '是否可预约',
  `sort_order` INT DEFAULT 0 COMMENT '排序',
  `status` TINYINT DEFAULT 1 COMMENT '状态',
  `created_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `deleted` TINYINT DEFAULT 0,
  PRIMARY KEY (`id`),
  KEY `idx_tenant_id` (`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='专家团队表';

-- ========================================
-- 7. 对话记录表
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
  `match_score` DECIMAL(5,4) DEFAULT NULL COMMENT '匹配度分数',
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
  KEY `idx_created_time` (`created_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='咨询对话记录表';

-- ========================================
-- 8. 数据统计表（按天统计）
-- ========================================
DROP TABLE IF EXISTS `daily_statistics`;
CREATE TABLE `daily_statistics` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `tenant_id` BIGINT NOT NULL COMMENT '租户 ID',
  `stat_date` DATE NOT NULL COMMENT '统计日期',
  `total_questions` INT DEFAULT 0 COMMENT '总提问数',
  `solved_questions` INT DEFAULT 0 COMMENT '已解决数',
  `unsolved_questions` INT DEFAULT 0 COMMENT '未解决数',
  `total_users` INT DEFAULT 0 COMMENT '总用户数',
  `new_users` INT DEFAULT 0 COMMENT '新增用户数',
  `avg_response_time` INT DEFAULT 0 COMMENT '平均响应时间（毫秒）',
  `avg_satisfaction` DECIMAL(3,2) DEFAULT 0 COMMENT '平均满意度',
  `top_questions` JSON DEFAULT NULL COMMENT '热门问题 TOP10（JSON）',
  `created_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `deleted` TINYINT DEFAULT 0,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_tenant_date` (`tenant_id`, `stat_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='每日统计表';

-- ========================================
-- 插入示例数据（政务服务场景）
-- ========================================

-- 示例租户
INSERT INTO `tenant_info` (`tenant_name`, `tenant_code`, `industry_type`, `contact_person`, `contact_phone`, `theme_color`, `welcome_message`) VALUES
('XX 市政务服务中心', 'gov_demo_001', 'government', '张主任', '13800138000', '#1890ff', '您好，XX 市政务服务中心很高兴为您服务！');

-- 示例企业信息
INSERT INTO `company_profile` (`tenant_id`, `company_name`, `company_short_name`, `founded_year`, `company_size`, `description`, `address`, `working_hours`) VALUES
(1, 'XX 市政务服务中心', '政务中心', 2015, '200-500 人', 'XX 市政务服务中心是市政府设立的综合性政务服务平台，提供一站式行政审批和公共服务。', 'XX 市 XX 区 XX 路 100 号市民之家', '周一至周日 9:00-17:00');

-- 示例服务网点
INSERT INTO `service_location` (`tenant_id`, `location_name`, `location_type`, `city`, `detail_address`, `opening_hours`, `traffic_guide`) VALUES
(1, '市民之家政务大厅', '总部', 'XX 市', 'XX 区 XX 路 100 号', '周一至周日 9:00-17:00', '地铁 1 号线市民之家站 C 口直达；公交 101、102 路市民之家站下车即到');

-- 示例知识分类
INSERT INTO `knowledge_category` (`tenant_id`, `category_name`, `parent_id`, `level`) VALUES
(1, '办事服务', 0, 1),
(1, '社保医保', 0, 1),
(1, '户籍办理', 0, 1),
(1, '居住证办理', 0, 1);

-- 示例知识条目
INSERT INTO `knowledge_item` (`tenant_id`, `category_id`, `title`, `question`, `keywords`, `answer`) VALUES
(1, 4, '居住证办理流程', '居住证怎么办理？', '居住证，办理，流程', '办理居住证需要携带本人身份证、租房合同或房产证、近期一寸照片到居住地派出所或政务服务中心公安窗口办理。也可通过"互联网 + 政务服务"平台在线申请。'),
(1, 2, '社保卡补办流程', '社保卡丢了怎么补办？', '社保卡，补办，丢失', '社保卡丢失后，请先拨打 12333 服务热线挂失，然后携带身份证到社保卡服务网点办理书面挂失和补办手续。补办费用 20 元，7-15 个工作日可领取新卡。');
