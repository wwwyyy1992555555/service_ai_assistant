# AI智库企业咨询平台

## 📖 项目简介

AI智库企业咨询平台是一个面向中小企业及政府机构的SaaS化智能客服系统，提供零代码知识库搭建、智能问答和多租户管理功能。该平台采用现代化的技术架构，支持多租户数据隔离，帮助企业快速构建专属的智能客服体系，降低人工客服成本，提升服务效率。

## 🎯 核心价值

- **降本增效**：替代传统热线咨询，7×24小时智能应答常见问题
- **知识沉淀**：统一管理企业内部文档，实现知识的结构化存储与检索
- **多租户支持**：一套系统服务多个企业，数据完全隔离，保障信息安全
- **灵活部署**：支持SaaS云端部署和私有化部署，满足不同客户需求
- **AI驱动**：集成大模型能力，提供智能化的语义理解和自然语言交互

## 🏗️ 技术架构

### 后端技术栈
- **核心框架**：Java 17 + Spring Boot 3.0.2
- **持久层**：MyBatis-Plus 3.5.3.1 + MySQL 8.0.33
- **缓存层**：Redis 6.x（会话管理、热点数据缓存）
- **连接池**：Druid 1.2.16（数据库连接监控与优化）
- **API文档**：Springdoc OpenAPI 2.0.2（Swagger UI）
- **工具类库**：Hutool 5.8.16、Fastjson2 2.0.25、Lombok 1.18.26
- **邮件服务**：Spring Boot Mail
- **Excel处理**：EasyExcel 3.3.2

### 前端技术栈
- **管理后台**：Vue3 + Element Plus
- **用户界面**：原生HTML/CSS/JS
- **UI组件**：Element Plus

### AI能力
- **大模型集成**：预留国产大模型API接口（阿里云通义千问/百度文心一言/智谱GLM等）
- **智能问答**：基于知识库的语义匹配，支持上下文理解
- **联网搜索**：支持实时联网获取最新信息

## 🚀 核心功能模块

### 1. 多租户管理体系
- 租户注册与管理（企业信息配置、Logo/主题色自定义）
- 租户状态控制（启用/禁用/过期管理）
- 行业类型分类管理
- 租户配置个性化（欢迎语、客服联系方式等）

### 2. 权限与安全体系
- 三级角色权限：超级管理员(tenant_id=0)、普通管理员(role_level=1)、操作员(role_level=2)
- JWT Token认证机制
- 密码加密存储（BCrypt）
- 水平权限校验（租户间数据隔离）
- 垂直权限校验（角色等级控制）

### 3. 知识库管理系统
- 多级分类管理（支持树形结构）
- 知识条目CRUD操作
- 富文本内容编辑
- 关键词检索与全文搜索
- Excel批量导入导出
- 浏览次数统计

### 4. 智能问答引擎
- 基于知识库的语义匹配
- 大模型API集成（可配置开关）
- 对话历史记录
- 满意度评价收集
- 转人工标记功能
- 礼貌用语识别与回复

### 5. 数据统计与分析
- 今日咨询量统计
- 问题解决率分析
- 热门问题TOP10排行
- 满意度评分统计
- 多维度数据可视化

### 6. 用户管理模块
- 用户CRUD操作
- 角色分配与权限管理
- 用户状态控制
- 登录日志记录

## 📁 项目结构

```
service_ai_assistant/
├── src/main/java/com/myproject/service_ai_assistant/
│   ├── annotation/          # 自定义注解
│   ├── aspect/              # AOP切面
│   ├── common/              # 通用工具类
│   │   ├── ExcelUtil.java   # Excel处理工具
│   │   ├── LevelCode.java   # 权限枚举
│   │   ├── PasswordUtil.java # 密码工具
│   │   ├── Result.java      # 统一返回结果
│   │   └── SimilarityUtil.java # 相似度计算
│   ├── config/              # 配置类
│   ├── context/             # 上下文管理
│   ├── controller/          # 控制器层
│   │   ├── AuthController.java       # 认证授权
│   │   ├── ConsultController.java    # 智能问答
│   │   ├── KnowledgeController.java  # 知识库管理
│   │   ├── TenantInfoController.java # 租户管理
│   │   └── UserController.java       # 用户管理
│   ├── dto/                 # 数据传输对象
│   ├── entity/              # 实体类
│   ├── enums/               # 枚举类
│   ├── exception/           # 异常处理
│   ├── interceptor/         # 拦截器
│   ├── mapper/              # 数据访问层
│   └── service/             # 业务逻辑层
├── src/main/resources/
│   ├── db/                  # 数据库脚本
│   ├── mapper/              # MyBatis映射文件
│   ├── static/              # 静态资源
│   │   ├── admin.html       # 管理后台
│   │   ├── chat.html        # 聊天界面
│   │   ├── css/             # 样式文件
│   │   └── js/              # JavaScript文件
│   └── application.yml      # 配置文件
└── pom.xml                  # Maven配置
```

## 🔧 快速启动

### 前置要求
- JDK 17+
- MySQL 8.0+
- Redis 5.0+ (推荐使用 5.0 或 6.x)
- Maven 3.6+

### 1. 数据库初始化

```bash
# 登录 MySQL
mysql -u root -p

# 执行初始化脚本
source src/main/resources/db/init.sql
```

### 2. 配置修改

编辑 `src/main/resources/application-dev.yml`：

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/ai_think_tank?useUnicode=true&characterEncoding=utf8&useSSL=false&serverTimezone=Asia/Shanghai
    username: root          # 修改为你的数据库用户名
    password: your_password # 修改为你的数据库密码
  
  redis:
    host: localhost         # 修改为你的 Redis 地址
    port: 6379
```

### 3. 启动项目

```bash
# 编译打包
mvn clean package -DskipTests

# 运行应用
java -jar target/service_ai_assistant-0.0.1-SNAPSHOT.war

# 或者直接运行
mvn spring-boot:run
```

### 4. 访问系统

启动成功后，访问以下地址：

- **管理后台**: http://localhost:8080/static/admin.html
- **用户聊天界面**: http://localhost:8080/static/chat.html
- **API 文档**: http://localhost:8080/swagger-ui.html

### 5. 测试接口

使用 Postman 或 API 文档测试智能问答接口：

```bash
POST http://localhost:8080/api/consult/ask
Content-Type: application/json

{
  "sessionId": "test_session_001",
  "question": "居住证怎么办理？",
  "tenantId": 1,
  "deviceType": "web"
}
```

## 📊 应用场景

- **小微企业与创业团队**：低成本搭建内部知识库，提升员工培训效率
- **政务服务中心**：替代传统热线咨询，7×24小时智能应答民生问题
- **律师事务所**：沉淀法律案例与法规条文，快速响应客户法律咨询
- **医院与医疗机构**：导诊咨询服务，减轻护士台工作压力
- **社区街道办**：居民办事指南查询，政策宣传与信息推送
- **教育机构**：招生咨询自动化，课程资料管理与学员答疑
- **物业公司**：业主常见问题解答，报修流程指引
- **培训/咨询机构**：课程体系沉淀，学员自助查询学习资料

## 💼 商业模式

### SaaS订阅制
- **基础版**：按坐席数收费（¥99/月/坐席），包含10GB存储空间
- **专业版**：按存储空间收费（¥299/月/50GB），支持自定义域名与Logo
- **企业版**：定制化服务（¥999/月起），专属技术支持与私有化部署选项

### 私有化部署
- **一次性授权费**：¥50,000 - ¥200,000（根据规模定制）
- **年服务费**：授权费的15%-20%（包含升级、维护、技术支持）

### 增值服务
- AI模型调用费用代付（按实际用量结算）
- 数据迁移与初始化服务（¥5,000 - ¥20,000）
- 定制化开发（人天计费 ¥2,000/人天）

## 🤝 贡献指南

欢迎提交Issue和Pull Request来改进本项目。在贡献代码前，请确保：

1. 遵循项目的代码规范
2. 添加必要的单元测试
3. 更新相关文档
4. 保持向后兼容性

## 📄 许可证

本项目采用 MIT 许可证 - 查看 [LICENSE](LICENSE) 文件了解详情

## 📞 联系方式

- **姓名**：吴燚
- **电话**：13912355227
- **微信/邮箱**：814218101@qq.com
- **GitHub**：https://github.com/wwwyyy1992555555/service_ai_assistant

## 🙏 致谢

感谢所有为本项目做出贡献的开发者和使用者！