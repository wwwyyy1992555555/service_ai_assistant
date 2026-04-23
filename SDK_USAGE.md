# 智能客服 Widget SDK 使用指南

## 快速开始

### 1. 基础引入

在您的网站 HTML 中添加以下代码:

```html
<script src="http://your-domain/sdk/chat-widget.js" data-tenant-code="YOUR_TENANT_CODE"></script>
```

**参数说明:**
- `data-tenant-code`: 租户编码(必填),由平台分配

### 2. 配置选项

在引入 SDK **之前**,定义全局配置对象:

```html
<script>
window.ChatWidgetConfig = {
    noButton: false,      // 是否隐藏内置悬浮按钮
    width: '450px',       // 聊天窗口宽度(支持 px、%、vw、vh)
    height: '650px'       // 聊天窗口高度(支持 px、%、vw、vh)
};
</script>
<script src="http://your-domain/sdk/chat-widget.js" data-tenant-code="YOUR_TENANT_CODE"></script>
```

**配置优先级:** 配置对象 > script属性 > 默认值

### 3. 自定义按钮模式

如果不想使用SDK内置的悬浮按钮,可以设置 `noButton: true`,然后自己创建按钮:

```html
<script>
window.ChatWidgetConfig = {
    noButton: true,
    width: '500px',
    height: '700px'
};
</script>
<script src="http://your-domain/sdk/chat-widget.js" data-tenant-code="YOUR_TENANT_CODE"></script>

<!-- 自定义按钮 -->
<button onclick="ChatWidget.open()">在线客服</button>
<button onclick="ChatWidget.toggle()">切换窗口</button>
```

## API 方法

SDK 初始化后,会暴露全局对象 `window.ChatWidget`,提供以下方法:

### ChatWidget.open()

打开聊天窗口

```javascript
ChatWidget.open();
```

### ChatWidget.close()

关闭聊天窗口

```javascript
ChatWidget.close();
```

### ChatWidget.toggle()

切换窗口显示/隐藏状态

```javascript
ChatWidget.toggle();
```

## 完整示例

### 示例1: 默认模式(使用SDK按钮)

```html
<!DOCTYPE html>
<html>
<head>
    <title>我的网站</title>
</head>
<body>
    <h1>欢迎访问</h1>
    
    <!-- 引入SDK -->
    <script src="http://your-domain/sdk/chat-widget.js" data-tenant-code="tenant_001"></script>
</body>
</html>
```

### 示例2: 自定义尺寸

```html
<script>
window.ChatWidgetConfig = {
    width: '600px',
    height: '800px'
};
</script>
<script src="http://your-domain/sdk/chat-widget.js" data-tenant-code="tenant_001"></script>
```

### 示例3: 完全自定义

```html
<script>
window.ChatWidgetConfig = {
    noButton: true,
    width: '50%',
    height: '80vh'
};
</script>
<script src="http://your-domain/sdk/chat-widget.js" data-tenant-code="tenant_001"></script>

<!-- 页面任意位置的按钮 -->
<div class="custom-chat-button" onclick="ChatWidget.open()">
    💬 联系客服
</div>
```

## 注意事项

1. **租户编码**: `data-tenant-code` 是必填项,请联系平台获取
2. **配置顺序**: `window.ChatWidgetConfig` 必须在引入 SDK **之前**定义
3. **跨域问题**: SDK 会自动处理跨域,无需额外配置
4. **移动端适配**: SDK 已内置响应式适配,移动端自动全屏显示
5. **会话持久化**: 聊天记录会自动保存在浏览器 LocalStorage 中(最近10条)

## 调试

访问测试页面验证集成效果:

```
http://your-domain/sdk-test-law-firm.html
```

测试页面包含:
- SDK 调试面板
- API 状态检测
- 功能测试按钮

## 常见问题

### Q: 聊天窗口不显示?

A: 检查以下几点:
1. `data-tenant-code` 是否正确
2. 浏览器控制台是否有报错
3. 租户是否已在平台激活

### Q: 如何修改窗口位置?

A: 目前不支持自定义位置,窗口固定在右下角。如需调整,可联系平台定制。

### Q: 聊天记录保存多久?

A: 默认保存最近10条对话,存储在浏览器 LocalStorage 中,除非用户手动清除缓存,否则长期有效。

### Q: 支持多个租户吗?

A: 每个页面只能引入一个租户的SDK。如需切换租户,需要重新加载页面并更改 `data-tenant-code`。

## 技术支持

如有问题,请联系平台技术支持或查阅平台文档中心。
