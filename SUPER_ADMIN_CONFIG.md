# 超级管理员配置说明

## 📋 概述

系统启动时会自动创建超级管理员账号（tenant_id=0, role_level=0），该账号拥有全平台的最高管理权限。

## 🔧 配置方式

### 方式一：修改 application.yml（开发环境推荐）

在 `src/main/resources/application.yml` 中配置：

```yaml
app:
  super-admin:
    username: platform_admin        # 超级管理员用户名
    password: Admin@2024#Secure     # 超级管理员密码
    real-name: 平台超级管理员       # 真实姓名
```

### 方式二：设置环境变量（生产环境推荐）

```bash
# Linux/Mac
export SUPER_ADMIN_USERNAME=platform_admin
export SUPER_ADMIN_PASSWORD=Admin@2024#Secure
export SUPER_ADMIN_REAL_NAME=平台超级管理员

# Windows PowerShell
$env:SUPER_ADMIN_USERNAME="platform_admin"
$env:SUPER_ADMIN_PASSWORD="Admin@2024#Secure"
$env:SUPER_ADMIN_REAL_NAME="平台超级管理员"
```

### 方式三：使用 .env 文件

1. 复制 `.env.example` 为 `.env`
2. 修改其中的配置项
3. 启动应用时加载环境变量

## 🔐 安全建议

### 1. 密码强度要求
- 最小长度：8 位
- 必须包含：数字、字母
- 建议使用：特殊字符（如 @#$%^&*）
- 避免使用：常见弱密码（123456、password 等）

### 2. 生产环境最佳实践
- ✅ 使用环境变量或密钥管理服务（如 AWS Secrets Manager、HashiCorp Vault）
- ✅ 定期更换密码（建议每 90 天）
- ✅ 启用双因素认证（2FA）
- ❌ 不要将密码硬编码在代码或配置文件中
- ❌ 不要使用默认密码

### 3. 示例强密码
```
Admin@2024#Secure
Platform#2024!Admin
Super@Admin_2024
```

## 🚀 首次登录

1. 启动应用后，查看日志确认超级管理员已创建：
   ```
   【用户初始化】超级管理员创建成功：username=platform_admin, realName=平台超级管理员
   ```

2. 访问管理后台：http://localhost:8080/static/admin.html

3. 使用以下信息登录：
   - **租户ID**：0（或留空）
   - **用户名**：platform_admin（或您配置的用户名）
   - **密码**：Admin@2024#Secure（或您配置的密码）

## ⚠️ 注意事项

1. **唯一性**：系统中只能有一个 tenant_id=0 的超级管理员账号
2. **不可删除**：超级管理员账号不能被删除或禁用
3. **权限范围**：超级管理员可以管理所有租户的数据，但不属于任何租户
4. **密码修改**：登录后建议立即修改默认密码

## 📝 技术实现

- **配置类**：`SuperAdminConfig.java`（读取配置文件）
- **初始化器**：`UserInitializer.java`（应用启动时自动创建）
- **密码加密**：BCrypt（单向哈希，不可逆）
- **触发时机**：Spring Boot 应用启动完成后（CommandLineRunner）

## 🔍 验证配置

启动应用后，可以通过以下方式验证超级管理员是否创建成功：

```sql
-- 查询超级管理员账号
SELECT * FROM user_info WHERE tenant_id = 0 AND role_level = 0;
```

预期结果：
```
id | tenant_id | username      | real_name    | role_level | status
---|-----------|---------------|--------------|------------|-------
1  | 0         | platform_admin| 平台超级管理员 | 0          | 1
```
