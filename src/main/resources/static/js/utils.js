/**
 * 工具函数库
 */

// API 请求封装
async function apiRequest(url, options = {}) {
    try {
        const response = await fetch(url, {
            ...options,
            headers: {
                'Content-Type': 'application/json',
                ...options.headers
            }
        });
        
        const result = await response.json();
        
        if (result.code === 200) {
            return result.data;
        } else {
            console.error('API 错误:', result.message);
            throw new Error(result.message || '请求失败');
        }
    } catch (error) {
        console.error('请求异常:', error);
        throw error;
    }
}

// GET 请求封装
async function get(url) {
    return apiRequest(url);
}

// POST 请求封装
async function post(url, data) {
    return apiRequest(url, {
        method: 'POST',
        body: JSON.stringify(data)
    });
}

// PUT 请求封装
async function put(url, data) {
    return apiRequest(url, {
        method: 'PUT',
        body: JSON.stringify(data)
    });
}

// DELETE 请求封装
async function deleteRequest(url) {
    return apiRequest(url, {
        method: 'DELETE'
    });
}

// 格式化数字（添加千分位）
function formatNumber(num) {
    return num.toString().replace(/\B(?=(\d{3})+(?!\d))/g, ',');
}

// 格式化日期
function formatDate(date, format = 'YYYY-MM-DD HH:mm:ss') {
    const d = new Date(date);
    const year = d.getFullYear();
    const month = String(d.getMonth() + 1).padStart(2, '0');
    const day = String(d.getDate()).padStart(2, '0');
    const hours = String(d.getHours()).padStart(2, '0');
    const minutes = String(d.getMinutes()).padStart(2, '0');
    const seconds = String(d.getSeconds()).padStart(2, '0');
    
    return format
        .replace('YYYY', year)
        .replace('MM', month)
        .replace('DD', day)
        .replace('HH', hours)
        .replace('mm', minutes)
        .replace('ss', seconds);
}

// 防抖函数
function debounce(func, wait) {
    let timeout;
    return function executedFunction(...args) {
        const later = () => {
            clearTimeout(timeout);
            func(...args);
        };
        clearTimeout(timeout);
        timeout = setTimeout(later, wait);
    };
}

// 节流函数
function throttle(func, limit) {
    let inThrottle;
    return function(...args) {
        if (!inThrottle) {
            func.apply(this, args);
            inThrottle = true;
            setTimeout(() => inThrottle = false, limit);
        }
    };
}

// 分类字典（根据数据库 init.sql 中的分类数据）
const CATEGORY_MAP = {
    1: '办事服务',
    2: '社保医保',
    3: '户籍办理',
    4: '居住证办理',
    5: '公积金',
    6: '工商登记',
    7: '税务服务',
    8: '医疗卫生'
};

// 获取分类名称
function getCategoryName(categoryId) {
    return CATEGORY_MAP[categoryId] || '未知分类';
}

// ==================== 系统设置相关 API ====================

/**
 * 获取系统配置
 */
async function loadSystemConfig(tenantId) {
    try {
        const response = await fetch(`/api/settings/get?tenantId=${tenantId}`);
        const result = await response.json();
        
        if (result.code === 200) {
            return result.data;
        } else {
            console.error('获取系统配置失败:', result.message);
            return null;
        }
    } catch (error) {
        console.error('获取系统配置异常:', error);
        return null;
    }
}

/**
 * 保存系统配置
 */
async function saveSystemConfig(tenantId, config) {
    try {
        const response = await fetch(`/api/settings/save?tenantId=${tenantId}`, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify(config)
        });
        
        const result = await response.json();
        
        if (result.code === 200) {
            return { success: true };
        } else {
            return { success: false, message: result.message || '保存失败' };
        }
    } catch (error) {
        console.error('保存系统配置异常:', error);
        return { success: false, message: '网络错误' };
    }
}
