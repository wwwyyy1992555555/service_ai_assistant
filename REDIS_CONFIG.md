# Redis 5.0 配置说明

## ✅ 已完成适配

项目已针对 Redis 5.0 进行了配置优化：

### 1. application-dev.yml 配置

```yaml
spring:
  redis:
    host: localhost
    port: 6379
    database: 0
    timeout: 5000ms
    lettuce:
      pool:
        max-active: 8
        max-idle: 8
        min-idle: 0
        max-wait: -1ms  # 新增：等待超时设置
```

### 2. 连接池说明

Redis 5.0 使用 **Lettuce** 作为默认连接客户端：
- Lettuce 是可伸缩的响应式 Redis 客户端
- 支持线程安全的异步操作
- 内置连接池管理

### 3. 测试 Redis 连接

启动项目后，访问以下接口测试 Redis 连接：

```bash
GET http://localhost:8080/api/test/redis/ping
```

**预期响应：**
```json
{
  "code": 200,
  "message": "操作成功",
  "data": "Redis 连接正常！",
  "timestamp": 1704067200000
}
```

### 4. Redis 5.0 vs 6.0 差异

| 特性 | Redis 5.0 | Redis 6.0 | 本项目影响 |
|------|-----------|-----------|----------|
| ACL 权限控制 | ❌ 不支持 | ✅ 支持 | 无影响（项目未使用） |
| SSL 加密连接 | ⚠️ 有限支持 | ✅ 完全支持 | 无影响（内网使用） |
| 新数据类型 | Stream | - | 无影响 |
| 性能 | 优秀 | 提升 15% | 足够使用 |

### 5. 如果连接失败

**常见错误 1：Connection refused**
```bash
# 检查 Redis 是否启动
redis-cli ping
# 应返回：PONG
```

**常见错误 2：Timeout**
```yaml
# 增加超时时间
spring:
  redis:
    timeout: 10000ms  # 增加到 10 秒
```

**常见错误 3：认证失败**
```yaml
# 如果设置了密码
spring:
  redis:
    password: your_password
```

### 6. Redis 安装（Windows）

如果使用 Windows 开发环境：

```bash
# 方式 1：使用 WSL
wsl sudo apt-get install redis-server

# 方式 2：使用 Docker
docker run -d -p 6379:6379 --name redis-5 redis:5

# 方式 3：下载 Windows 版本
# https://github.com/microsoftarchive/redis/releases
```

### 7. 验证配置生效

启动项目后查看日志：

```
Using Redis connection factory: org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory
Redis connection initialized successfully
```

---

## 🎯 下一步建议

虽然 Redis 5.0 已经足够使用，但如果升级到 6.0+ 可以获得：
- 更好的 ACL 权限管理
- 原生 SSL/TLS 支持
- 约 15% 的性能提升

**升级建议：**
- 开发环境：继续使用 5.0（稳定够用）
- 生产环境：建议使用 6.0+（功能更完善）
