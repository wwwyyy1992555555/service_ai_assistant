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
        console.error('加载分类列表失败:', error);
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
        console.error('保存分类失败:', error);
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
        console.error('删除分类失败:', error);
        return { success: false, message: '网络错误' };
    }
}

// 加载知识列表
async function loadKnowledgeList(current = 1, size = 10) {
    try {
        const response = await fetch(`/api/knowledge/list?tenantId=1&current=${current}&size=${size}`);
        const result = await response.json();
        
        console.log('加载知识列表:', result);
        
        if (result.code === 200) {
            return {
                records: result.data.records || [],
                total: result.data.total || 0
            };
        } else {
            console.error('加载知识列表失败:', result.message);
        }
    } catch (error) {
        console.error('加载知识列表失败:', error);
    }
    return { records: [], total: 0 };
}

// 搜索知识
async function searchKnowledge(keyword, current = 1, size = 10) {
    try {
        const response = await fetch(`/api/knowledge/list?tenantId=1&current=${current}&size=${size}&keyword=${encodeURIComponent(keyword)}`);
        const result = await response.json();
        
        if (result.code === 200) {
            return {
                records: result.data.records || [],
                total: result.data.total || 0
            };
        }
    } catch (error) {
        console.error('搜索知识失败:', error);
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
        console.error('保存失败:', error);
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
        console.error('删除失败:', error);
        return { success: false, message: '网络错误' };
    }
}
