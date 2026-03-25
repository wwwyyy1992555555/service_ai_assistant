/**
 * 数据看板模块
 */

// 加载统计数据
async function loadDashboard() {
    try {
        const response = await fetch('/api/statistics/dashboard?tenantId=1');
        const result = await response.json();
        
        if (result.code === 200 && result.data) {
            return result.data;
        }
    } catch (error) {
        console.error('加载看板数据失败:', error);
    }
    return null;
}

// 加载热门问题
async function loadHotQuestions(limit = 10) {
    try {
        const response = await fetch(`/api/statistics/hot-questions?tenantId=1&limit=${limit}`);
        const result = await response.json();
        
        if (result.code === 200) {
            return result.data || [];
        }
    } catch (error) {
        console.error('热门问题加载失败:', error);
    }
    return [];
}

// 渲染统计数据
function renderStats(stats) {
    return {
        todayConsultations: stats.todayConsultations || 0,
        consultationGrowth: stats.consultationGrowth || 0,
        solveRate: stats.solveRate || 0,
        solveRateGrowth: stats.solveRateGrowth || 0,
        avgSatisfaction: stats.avgSatisfaction || 0,
        knowledgeCount: stats.knowledgeCount || 0,
        knowledgeCategories: stats.knowledgeCategories || 0
    };
}
