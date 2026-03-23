# AI 智库企业咨询平台

## 🚀 快速启动指南

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
java -jar target/service_ai_assistant-0.0.1-SNAPSHOT.jar

# 或者直接运行
mvn spring-boot:run
```

### 4. 访问系统

启动成功后，访问以下地址：

- **管理后台**: http://localhost:8080/static/admin.html
- **用户聊天界面**: http://localhost:8080/static/chat.html
- **API 文档**: http://localhost:8080/api/doc.html

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

## 📊 功能特性

✅ **多租户支持** - 不同企业数据隔离  
✅ **智能问答** - 基于知识库的语义匹配  
✅ **知识库管理** - 支持富文本、附件  
✅ **对话记录** - 完整的咨询历史  
✅ **数据统计** - 热门问题、解决率分析  
✅ **精美界面** - 现代化的管理后台和用户端  

## 🎯 下一步计划

- [ ] 集成文心一言/通义千问 AI 模型
- [ ] 实现向量语义搜索（Milvus/PGVector）
- [ ] 添加文件上传功能（Word/Excel 批量导入）
- [ ] 完善多环境配置（生产/测试）
- [ ] Docker 容器化部署
- [ ] WebSocket 实时消息推送

## 💡 技术支持

如有问题，请查看日志文件或联系开发团队。
