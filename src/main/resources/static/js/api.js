/**
 * API 接口封装
 * 所有与后端的交互都通过此文件管理
 */

const API_BASE = '/api';

/**
 * 通用请求封装
 * @param {string} url - 请求 URL
 * @param {object} options - 请求选项
 * @returns {Promise} - 请求结果
 */
async function request(url, options = {}) {
    const defaultOptions = {
        method: 'GET',
        headers: {
            'Content-Type': 'application/json',
        },
    };

    const config = { ...defaultOptions, ...options };
    
    // 添加认证 token
    const token = localStorage.getItem('token');
    if (token) {
        config.headers['Authorization'] = `Bearer ${token}`;
    }

    try {
        const response = await fetch(url, config);
        const data = await response.json();

        if (data.code !== 200 && data.code !== 0) {
            throw new Error(data.message || '请求失败');
        }

        // 直接返回完整的响应数据，让调用方自己处理
        return data;
    } catch (error) {
        throw error;
    }
}

/**
 * GET 请求
 */
function get(url, params = {}) {
    const queryString = new URLSearchParams(params).toString();
    const fullUrl = queryString ? `${url}?${queryString}` : url;
    return request(fullUrl);
}

/**
 * POST 请求
 */
function post(url, data = {}) {
    return request(url, {
        method: 'POST',
        body: JSON.stringify(data),
    });
}

/**
 * DELETE 请求
 */
function del(url) {
    return request(url, {
        method: 'DELETE',
    });
}

// ==================== 数据看板接口 ====================

/**
 * 获取统计数据
 */
async function loadDashboard() {
    const result = await get(`${API_BASE}/statistics/dashboard`, { 
        tenantId: 1 
    });
    return result.data || {};
}

/**
 * 获取热门问题
 * @param {number} limit - 数量限制
 * @returns {Promise<Array>}
 */
async function loadHotQuestions(limit = 10) {
    // 调用后端专门的热门问题接口：/api/statistics/hot-questions
    const result = await get(`${API_BASE}/statistics/hot-questions`, { 
        tenantId: 1, 
        limit: limit 
    });
    return result.data || [];
}

// ==================== 知识库接口 ====================

/**
 * 获取知识列表
 * @param {number} current - 当前页码
 * @param {number} size - 每页大小
 * @param {string} keyword - 搜索关键词
 * @param {number|null} publishStatus - 发布状态
 * @param {number|null} isTop - 是否置顶
 * @param {number|null} categoryId - 分类 ID（-1 表示无分类）
 * @returns {Promise<{records: Array, total: number}>}
 */
async function loadKnowledgeList(current, size, keyword, publishStatus, isTop, categoryId) {
    // 确保参数类型正确：publishStatus 和 isTop 必须是 Integer
    const params = {
        tenantId: 1,
        current: current || 1,
        size: size || 10,
    };
    
    // 只有当 keyword 有值时才传递
    if (keyword) {
        params.keyword = keyword;
    }
    
    // publishStatus 和 isTop 必须是 Integer (0 或 1)，null 或 undefined 时不传
    if (publishStatus !== null && publishStatus !== undefined) {
        params.publishStatus = parseInt(publishStatus);
    }
    
    if (isTop !== null && isTop !== undefined) {
        params.isTop = parseInt(isTop);
    }
    
    // 添加分类参数
    if (categoryId !== null && categoryId !== undefined) {
        params.categoryId = categoryId;
    }
    
    const result = await get(`${API_BASE}/knowledge/list`, params);
    // 后端返回格式：{ code: 200, data: { records: [...], total: 100 } }
    return result.data || { records: [], total: 0 };
}

/**
 * 搜索知识
 * 注意：后端没有 /knowledge/search 接口，使用 /knowledge/list 代替
 * @param {string} keyword - 搜索关键词
 * @param {number} current - 当前页码
 * @param {number} size - 每页大小
 * @param {number|null} publishStatus - 发布状态
 * @param {number|null} isTop - 是否置顶
 * @param {number|null} categoryId - 分类 ID（-1 表示无分类）
 * @returns {Promise<{records: Array, total: number}>}
 */
async function searchKnowledge(keyword, current, size, publishStatus, isTop, categoryId) {
    const params = {
        tenantId: 1,
        keyword: keyword || '',
        current: current || 1,
        size: size || 10,
    };

    // publishStatus 和 isTop 必须是 Integer (0 或 1)，null 或 undefined 时不传
    if (publishStatus !== null && publishStatus !== undefined) {
        params.publishStatus = parseInt(publishStatus);
    }

    if (isTop !== null && isTop !== undefined) {
        params.isTop = parseInt(isTop);
    }

    // 添加分类参数
    if (categoryId !== null && categoryId !== undefined) {
        params.categoryId = categoryId;
    }

    // 使用 /knowledge/list 接口代替（后端支持 keyword 参数进行搜索）
    const result = await get(`${API_BASE}/knowledge/list`, params);
    return result.data || { records: [], total: 0 };
}

/**
 * 获取分类列表
 * @returns {Promise<Array>}
 */
async function loadCategories() {
    // 后端接口是 /api/knowledge/categories
    const result = await get(`${API_BASE}/knowledge/categories`);
    return result.data || [];
}

/**
 * 添加分类
 */
async function addCategory(data) {
    const result = await post(`${API_BASE}/knowledge/category`, data);
    return result.data;
}

/**
 * 更新分类
 */
async function updateCategory(data) {
    // 后端使用 POST /category 同时处理新增和编辑
    const result = await post(`${API_BASE}/knowledge/category`, data);
    return result.data;
}

/**
 * 删除分类
 */
async function deleteCategory(id) {
    const result = await request(`${API_BASE}/knowledge/category/${id}`, {
        method: 'DELETE',
    });
    return result.data;
}

/**
 * 添加知识
 */
async function addKnowledge(data) {
    // 后端使用 POST /api/knowledge（@PostMapping）
    const result = await post(`${API_BASE}/knowledge`, data);
    return result.data;
}

/**
 * 更新知识
 */
async function updateKnowledge(data) {
    // 后端使用 PUT /api/knowledge（@PutMapping）
    const result = await request(`${API_BASE}/knowledge`, {
        method: 'PUT',
        body: JSON.stringify(data),
    });
    return result.data;
}

/**
 * 删除知识
 */
async function deleteKnowledge(id) {
    // 后端使用 DELETE /api/knowledge/{id}（@DeleteMapping("/{id}")）
    const result = await request(`${API_BASE}/knowledge/${id}`, {
        method: 'DELETE',
    });
    return result.data;
}

// ==================== 对话记录接口 ====================

/**
 * 获取对话记录列表
 * @returns {Promise<{records: Array, total: number}>}
 */
async function loadRecordsList(current, size) {
    const result = await get(`${API_BASE}/consult/list`, {
        tenantId: 1,
        current: current || 1,
        size: size || 10,
    });
    return result.data || { records: [], total: 0 };
}

/**
 * 搜索对话记录
 * @returns {Promise<{records: Array, total: number}>}
 */
async function searchRecords(keyword, current, size) {
    const result = await get(`${API_BASE}/consult/search`, {
        tenantId: 1,
        keyword: keyword || '',
        current: current || 1,
        size: size || 10,
    });
    return result.data || { records: [], total: 0 };
}

/**
 * 获取会话详情
 */
async function getSessionDetail(sessionId) {
    const result = await get(`${API_BASE}/consult/session/${sessionId}`);
    // 后端返回：{ code: 200, data: [...] }
    return result.data || [];
}

/**
 * 删除会话
 */
async function deleteSession(sessionId) {
    // 成功时后端返回：{ code: 200, data: null }
    await del(`${API_BASE}/consult/session/${sessionId}`);
    return true;
}

// ==================== 系统设置接口 ====================

/**
 * 获取系统配置
 */
async function loadSystemConfig(tenantId) {
    // 后端接口是 /api/settings/get
    const result = await get(`${API_BASE}/settings/get`, { tenantId });
    return result.data || {};
}

/**
 * 保存系统配置
 */
async function saveSystemConfig(tenantId, config) {
    // tenantId 需要作为 URL 参数传递，而不是放在 body 中
    const result = await post(`${API_BASE}/settings/save?tenantId=${tenantId}`, config);
    return result.data;
}

// 将 API 函数挂载到 window 对象，保持与现有代码兼容
window.loadDashboard = loadDashboard;
window.loadHotQuestions = loadHotQuestions;
window.loadKnowledgeList = loadKnowledgeList;
window.searchKnowledge = searchKnowledge;
window.loadCategories = loadCategories;
window.addCategory = addCategory;
window.updateCategory = updateCategory;
window.deleteCategory = deleteCategory;
window.addKnowledge = addKnowledge;
window.updateKnowledge = updateKnowledge;
window.deleteKnowledge = deleteKnowledge;
window.loadRecordsList = loadRecordsList;
window.searchRecords = searchRecords;
window.getSessionDetail = getSessionDetail;
window.deleteSession = deleteSession;
window.loadSystemConfig = loadSystemConfig;
window.saveSystemConfig = saveSystemConfig;
