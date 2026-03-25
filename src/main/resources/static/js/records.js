/**
 * 对话记录模块
 */

// 加载对话记录
async function loadRecords(limit = 10) {
    try {
        const response = await fetch(`/api/statistics/recent-records?tenantId=1&limit=${limit}`);
        const result = await response.json();
        
        if (result.code === 200) {
            return result.data || [];
        }
    } catch (error) {
        console.error('加载对话记录失败:', error);
    }
    return [];
}

// 搜索对话记录
async function searchRecords(keyword, current = 1, size = 10) {
    try {
        const response = await fetch(`/api/consult/search?tenantId=1&keyword=${encodeURIComponent(keyword)}&current=${current}&size=${size}`);
        const result = await response.json();
        
        if (result.code === 200) {
            return {
                records: result.data.records || [],
                total: result.data.total || 0
            };
        }
    } catch (error) {
        console.error('搜索对话记录失败:', error);
    }
    return { records: [], total: 0 };
}

// 加载对话记录（分页）
async function loadRecordsPage(current = 1, size = 10, searchKeyword = '') {
    try {
        let url;
        console.log('【loadRecordsPage】调用参数：current=', current, 'size=', size, 'searchKeyword=', searchKeyword);
        if (searchKeyword && searchKeyword.trim()) {
            // 有搜索关键词时，调用搜索接口
            url = `/api/consult/search?tenantId=1&keyword=${encodeURIComponent(searchKeyword)}&current=${current}&size=${size}`;
            console.log('【loadRecordsPage】使用搜索接口：', url);
        } else {
            // 无搜索关键词时，调用分页列表接口
            url = `/api/consult/list?tenantId=1&current=${current}&size=${size}`;
            console.log('【loadRecordsPage】使用列表接口：', url);
        }
        
        const response = await fetch(url);
        const result = await response.json();
        
        console.log('【loadRecordsPage】后端返回：', result);
        
        if (result.code === 200) {
            // 返回分页对象
            return {
                records: result.data.records || [],
                total: result.data.total || 0
            };
        }
    } catch (error) {
        console.error('加载对话记录失败:', error);
    }
    return { records: [], total: 0 };
}

// 删除对话记录（按会话删除）
async function deleteSession(sessionId) {
    try {
        const response = await fetch(`/api/consult/session/${sessionId}`, {
            method: 'DELETE'
        });
        const result = await response.json();
        
        if (result.code === 200) {
            return { success: true };
        } else {
            return { success: false, message: result.message || '删除失败' };
        }
    } catch (error) {
        console.error('删除会话失败:', error);
        return { success: false, message: '网络错误' };
    }
}

// 删除对话记录（保留兼容旧接口）
async function deleteRecord(id) {
    console.warn('deleteRecord 已废弃，请使用 deleteSession');
    return deleteSession(id); // 暂时兼容，实际不会用
}

// 获取会话详情
async function getSessionDetail(sessionId) {
    try {
        const response = await fetch(`/api/consult/session/${sessionId}`);
        const result = await response.json();
        
        if (result.code === 200) {
            return result.data || [];
        } else {
            return [];
        }
    } catch (error) {
        console.error('获取会话详情失败:', error);
        return [];
    }
}
