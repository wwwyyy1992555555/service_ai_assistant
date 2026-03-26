# 大模型配置说明

## 📋 功能说明

系统支持接入大模型（如阿里云通义千问、百度文心一言、Kimi 等），实现更智能的回复。

### 混合模式策略

```
用户提问
   ↓
1. 知识库匹配
   ├─ 高匹配（≥0.8） → 直接使用知识库答案（快速、准确、免费）
   ├─ 中匹配（0.5-0.8） → 大模型基于知识库润色（准确 + 自然）
   └─ 低匹配（<0.5） → 大模型自由回答（智能、灵活）
```

---

## ⚙️ 配置说明

### application.yml 配置

```yaml
app:
  llm:
    # 是否启用大模型（true=启用，false=关闭）
    enabled: false
    
    # 大模型服务商（目前支持：aliyun, baidu, kimi）
    provider: aliyun
    
    # API Key（从服务商获取）
    api-key: ${LLM_API_KEY:}
    
    # API Secret（从服务商获取）
    api-secret: ${LLM_API_SECRET:}
    
    # 模型名称
    model: qwen-turbo
    
    # 超时时间（秒）
    timeout: 30
    
    # 是否使用知识库增强（true=基于知识库回答，false=自由回答）
    knowledge-enhanced: true
```

---

## 🚀 快速开始

### 方式 1：环境变量配置（推荐）

```bash
# 设置环境变量
export LLM_API_KEY=your-api-key
export LLM_API_SECRET=your-api-secret

# 启动应用
java -jar service_ai_assistant.jar
```

### 方式 2：直接修改配置文件

```yaml
app:
  llm:
    enabled: true
    provider: aliyun
    api-key: sk-xxxxxxxxxxxxxx
    api-secret: xxxxxxxxxxxxxx
    model: qwen-turbo
    timeout: 30
    knowledge-enhanced: true
```

---

## 📊 开关控制

### 关闭大模型（仅使用知识库）

```yaml
app:
  llm:
    enabled: false
```

**效果：**
- ✅ 所有回复都从知识库匹配
- ✅ 不产生任何费用
- ❌ 无法回答知识库外的问题

---

### 开启大模型（混合模式）

```yaml
app:
  llm:
    enabled: true
    knowledge-enhanced: true  # 基于知识库增强
```

**效果：**
- ✅ 高匹配问题 → 知识库（快速、准确）
- ✅ 中匹配问题 → 大模型润色（自然）
- ✅ 低匹配问题 → 大模型回答（智能）

---

### 开启大模型（纯自由回答）

```yaml
app:
  llm:
    enabled: true
    knowledge-enhanced: false  # 不使用知识库增强
```

**效果：**
- ✅ 所有问题都由大模型回答
- ❌ 可能不够准确（没有知识库约束）
- ❌ 费用较高

---

## 💰 成本估算

### 场景 1：仅知识库（enabled: false）
- **费用：** 免费
- **适用：** 测试、演示、问题类型固定

### 场景 2：混合模式（enabled: true, knowledge-enhanced: true）
- **费用：** 约 ¥100-200/月（日活 1000 用户）
- **适用：** 生产环境、问题类型多样

### 场景 3：纯大模型（enabled: true, knowledge-enhanced: false）
- **费用：** 约 ¥300-500/月（日活 1000 用户）
- **适用：** 不推荐（成本高、准确性低）

---

## 🔧 接入真实大模型

当前代码使用的是**模拟回复**，实际使用时需要接入真实的大模型 API。

### 修改 LlmServiceImpl.java

```java
@Override
public String generateResponse(String question) {
    if (!llmConfig.isEnabled()) {
        return getDefaultResponse(question);
    }
    
    // TODO: 调用真实的大模型 API
    // 示例：阿里云通义千问
    return callAliyunLlm(question);
}

@Override
public String generateResponseWithKnowledge(String question, String knowledgeContext) {
    if (!llmConfig.isEnabled()) {
        return getDefaultResponse(question);
    }
    
    // TODO: 调用真实的大模型 API，并带上知识库上下文
    String prompt = "请基于以下资料回答：\n" + knowledgeContext + "\n问题：" + question;
    return callAliyunLlm(prompt);
}

// 实现具体的大模型调用方法
private String callAliyunLlm(String prompt) {
    // 调用阿里云通义千问 API
    // 文档：https://help.aliyun.com/zh/dashscope/
    // ...
}
```

---

## 📚 推荐大模型服务商

### 1. 阿里云 - 通义千问
- **官网：** https://www.aliyun.com/product/dashscope
- **价格：** 约 ¥0.008/次
- **优势：** 中文能力强、文档完善

### 2. 百度 - 文心一言
- **官网：** https://cloud.baidu.com/product/wenxinworkshop
- **价格：** 约 ¥0.012/次
- **优势：** 企业级服务、稳定性好

### 3. Kimi（月之暗面）
- **官网：** https://kimi.moonshot.cn/
- **价格：** 约 ¥0.005/次
- **优势：** 性价比高、响应速度快

---

## 🎯 最佳实践

### 1. 测试阶段
```yaml
enabled: false  # 先测试知识库匹配效果
```

### 2. 收集数据
- 统计匹配率（匹配成功数 / 总问题数）
- 收集未匹配到的问题
- 如果匹配率 < 60%，考虑接入大模型

### 3. 生产环境
```yaml
enabled: true
knowledge-enhanced: true  # 基于知识库增强
```

### 4. 监控优化
- 监控大模型调用次数和费用
- 定期补充知识库（减少大模型调用）
- 根据用户反馈调整匹配度阈值

---

## ❓ 常见问题

### Q1: 大模型会不会胡编乱造？
**A:** 使用 `knowledge-enhanced: true` 可以让大模型基于知识库回答，大幅减少"幻觉"。

### Q2: 费用会不会很高？
**A:** 混合模式下，60-80% 的问题用知识库（免费），只有 20-40% 调用大模型，成本可控。

### Q3: 如何切换不同的大模型？
**A:** 修改 `provider` 配置，并实现对应的 API 调用方法。

### Q4: 能否动态开关大模型？
**A:** 可以！修改配置后无需重启，后续可以添加动态刷新功能（@RefreshScope）。

---

## 📞 技术支持

如有问题，请联系开发团队或参考：
- 阿里云文档：https://help.aliyun.com/zh/dashscope/
- 百度智能云：https://cloud.baidu.com/doc/
- Kimi 开放平台：https://platform.moonshot.cn/
