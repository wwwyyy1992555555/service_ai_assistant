/**
 * 聊天页面业务逻辑模块
 */

/**
 * 从 URL 参数获取租户 ID
 * 支持访问方式：
 * - /chat.html?tenantId=1
 * - /chat.html (默认租户 ID=1)
 */
function getTenantIdFromUrl() {
    const params = new URLSearchParams(window.location.search);
    const tenantId = params.get('tenantId');
    return tenantId ? parseInt(tenantId) : 1; // 默认为 1
}

const API_BASE_URL = 'http://localhost:8080/api';
// 获取当前租户 ID（全局变量）
const CURRENT_TENANT_ID = getTenantIdFromUrl();

// 导出到全局作用域，供 HTML 中的 Vue 代码使用
window.API_BASE_URL = API_BASE_URL;
window.CURRENT_TENANT_ID = CURRENT_TENANT_ID;

// 调试日志：确认变量已正确导出
console.log('=== chat.js 加载完成 ===');
console.log('API_BASE_URL:', window.API_BASE_URL);
console.log('CURRENT_TENANT_ID:', window.CURRENT_TENANT_ID);

// 生成会话 ID
function generateSessionId() {
    return 'session_' + Date.now() + '_' + Math.random().toString(36).substr(2, 9);
}

// 获取当前时间
function getCurrentTime() {
    const now = new Date();
    return now.toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' });
}

// 滚动到底部
function scrollToBottom(container) {
    if (container) {
        container.scrollTop = container.scrollHeight;
    }
}

// 发送消息到 AI
async function sendMessageToAI(sessionId, question) {
    try {
        const response = await fetch(`${API_BASE_URL}/consult/ask`, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
            },
            body: JSON.stringify({
                sessionId: sessionId,
                question: question,
                tenantId: CURRENT_TENANT_ID, // 使用动态租户 ID
                deviceType: 'web'
            })
        });

        const result = await response.json();
        
        if (result.code === 200) {
            return {
                success: true,
                answer: result.data.answer,
                matchScore: result.data.matchScore || 0,
                knowledgeTitle: result.data.knowledgeTitle,
                categoryName: result.data.categoryName,
                viewCount: result.data.viewCount || 0,
                suggestedQuestions: result.data.suggestedQuestions || []
            };
        } else {
            throw new Error(result.message || '请求失败');
        }
    } catch (error) {
        return {
            success: false,
            answer: '抱歉，网络开小差了，请稍后再试。',
            matchScore: 0,
            knowledgeTitle: null,
            categoryName: null,
            viewCount: 0,
            suggestedQuestions: []
        };
    }
}

// 提交满意度评价
async function submitSatisfaction(messageId, satisfaction) {
    try {
        // TODO: 实现后端接口
        return true;
    } catch (error) {
        return false;
    }
}

// 加载热门问题
async function loadHotQuestions(limit = 4) {
    try {
        const response = await fetch(`${API_BASE_URL}/consult/hot-questions?tenantId=${CURRENT_TENANT_ID}&limit=${limit}`);
        const result = await response.json();
        
        if (result.code === 200 && result.data && result.data.length > 0) {
            return result.data.map(item => item.question);
        }
    } catch (error) {
        // 静默失败
    }
    return [];
}
