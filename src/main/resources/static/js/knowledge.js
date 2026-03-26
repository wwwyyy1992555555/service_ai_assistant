/**
 * 知识库管理模块
 */

// 加载分类列表
async function loadCategories() {
    try {
        const response = await fetch('/api/knowledge/categories');
        const result = await response.json();
        
        if (result.code === 200) {
            return result.data || [];
        }
    } catch (error) {
        // 静默失败
    }
    return [];
}

// 保存分类
async function saveCategory(data) {
    try {
        const response = await fetch('/api/knowledge/category', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify(data)
        });
        const result = await response.json();
        return result;
    } catch (error) {
        return { success: false, message: '网络错误' };
    }
}

// 删除分类
async function deleteCategory(id) {
    try {
        const response = await fetch(`/api/knowledge/category/${id}`, {
            method: 'DELETE'
        });
        const result = await response.json();
        return result;
    } catch (error) {
        return { success: false, message: '网络错误' };
    }
}

// 加载知识列表（支持筛选）
async function loadKnowledgeList(current = 1, size = 10, keyword = '', publishStatus = null, isTop = null) {
    try {
        let url = `/api/knowledge/list?tenantId=1&current=${current}&size=${size}`;
        
        // 添加筛选参数
        if (keyword && keyword.trim()) {
            url += `&keyword=${encodeURIComponent(keyword.trim())}`;
        }
        if (publishStatus !== null && publishStatus !== undefined) {
            url += `&publishStatus=${publishStatus}`;
        }
        if (isTop !== null && isTop !== undefined) {
            url += `&isTop=${isTop}`;
        }
        
        const response = await fetch(url);
        const result = await response.json();
        
        if (result.code === 200) {
            return {
                records: result.data.records || [],
                total: result.data.total || 0
            };
        }
    } catch (error) {
        // 静默失败
    }
    return { records: [], total: 0 };
}

// 搜索知识（支持筛选）
async function searchKnowledge(keyword, current = 1, size = 10, publishStatus = null, isTop = null) {
    try {
        let url = `/api/knowledge/list?tenantId=1&current=${current}&size=${size}&keyword=${encodeURIComponent(keyword)}`;
        
        // 添加筛选参数
        if (publishStatus !== null && publishStatus !== undefined) {
            url += `&publishStatus=${publishStatus}`;
        }
        if (isTop !== null && isTop !== undefined) {
            url += `&isTop=${isTop}`;
        }
        
        const response = await fetch(url);
        const result = await response.json();
        
        if (result.code === 200) {
            return {
                records: result.data.records || [],
                total: result.data.total || 0
            };
        }
    } catch (error) {
        // 静默失败
    }
    return { records: [], total: 0 };
}

// 保存知识（新增/编辑）
async function saveKnowledge(data) {
    try {
        const url = data.id ? '/api/knowledge' : '/api/knowledge';
        const method = data.id ? 'PUT' : 'POST';
        
        const response = await fetch(url, {
            method: method,
            headers: {
                'Content-Type': 'application/json',
            },
            body: JSON.stringify({
                ...data,
                tenantId: 1
            })
        });
        
        const result = await response.json();
        
        if (result.code === 200) {
            return { success: true, message: '保存成功' };
        } else {
            return { success: false, message: result.message || '保存失败' };
        }
    } catch (error) {
        return { success: false, message: '网络错误' };
    }
}

// 删除知识
async function deleteKnowledge(id) {
    try {
        const response = await fetch(`/api/knowledge/${id}`, {
            method: 'DELETE'
        });
        
        const result = await response.json();
        
        if (result.code === 200) {
            return { success: true, message: '删除成功' };
        } else {
            return { success: false, message: '删除失败' };
        }
    } catch (error) {
        return { success: false, message: '网络错误' };
    }
}
