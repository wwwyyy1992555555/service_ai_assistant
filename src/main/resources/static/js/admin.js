/**
 * AI 智库管理后台 - 主应用入口
 * 
 * 版本：v2.1 (重构完成版)
 * 最后更新：2026-03-27
 * 
 * 模块结构：
 * - utils.js - 工具函数库
 * - api.js - API 接口封装
 * - modules/knowledge.js - 知识库管理模块
 * 
 * CSS 结构：
 * - common.css - 全局公共样式
 * - modules/dashboard.css - 数据看板
 * - modules/knowledge.css - 知识库管理
 * - modules/records.css - 对话记录
 * - admin.css - 核心布局
 */

// ==================== 登录检查与初始化 ====================

const token = localStorage.getItem('token');
const userStr = localStorage.getItem('user');

if (!token || !userStr) {
    window.location.href = '/login.html';
}

const user = userStr ? JSON.parse(userStr) : null;

// 使用 window.Vue（由 HTML 中的 module 提供）
const { createApp, ref, reactive, computed, onMounted, watch, nextTick } = window.Vue;

const app = createApp({
    setup() {
        // 从 localStorage 获取用户信息
        const currentUser = JSON.parse(localStorage.getItem('user') || '{}');
        
        // 使用模块的状态和方法
        const userDisplayName = computed(() => {
            const name = currentUser.username || currentUser.realName || '用户';
            return name;
        });
        
        // ==================== 状态定义 ====================
        const currentMenu = ref('dashboard');
        const renderKey = ref(0);
        
        // 统计数据
        const stats = reactive({
            todayConsultations: 0,
            consultationGrowth: 0,
            solveRate: 0,
            solveRateGrowth: 0,
            avgSatisfaction: 0,
            knowledgeCount: 0,
            publishedCount: 0
        });
        
        // 热门问题
        const hotQuestions = ref([]);
        
        // 分类列表
        const categoryList = ref([]);
        
        // 分类字典（从后端加载）
        const categoryMap = ref({});
        
        // 知识详情
        const selectedKnowledge = ref({});
        const detailDialogVisible = ref(false);
        
        // ==================== 通用方法 ====================
        
        const getMenuName = () => {
            const names = {
                'dashboard': '数据看板',
                'categories': '分类管理',
                'knowledge': '知识库管理',
                'records': '对话记录',
                'settings': '系统设置'
            };
            return names[currentMenu.value];
        };
        
        const switchMenu = (menu) => {
            currentMenu.value = menu;
        };
        
        // ==================== 数据看板模块 ====================
        
        const loadDashboard = async () => {
            const data = await window.loadDashboard();
            if (data) {
                Object.assign(stats, data);
            }
        };
        
        const loadHotQuestions = async () => {
            try {
                const data = await window.loadHotQuestions(10);
                hotQuestions.value = Array.isArray(data) ? data : [];
                console.log('【热门问题】加载成功:', hotQuestions.value.length, '条');
            } catch (error) {
                console.error('【热门问题】加载失败:', error);
                hotQuestions.value = [];
            }
        };
        
        const handleLogout = async () => {
            try {
                await ElementPlus.ElMessageBox.confirm('确定要退出登录吗？', '提示', {
                    confirmButtonText: '确定',
                    cancelButtonText: '取消',
                    type: 'warning'
                });
                
                localStorage.removeItem('token');
                localStorage.removeItem('user');
                
                if (user && user.id) {
                    await fetch(`/api/auth/logout?userId=${user.id}`, {
                        method: 'POST'
                    });
                }
                
                ElementPlus.ElMessage.success('已退出登录');
                
                setTimeout(() => {
                    window.location.href = '/login.html';
                }, 500);
            } catch (error) {
                if (error !== 'cancel') {
                    // 用户取消退出
                }
            }
        };
        
        // 系统设置
        const settings = reactive({
            companyName: 'XX 市政务服务中心',
            welcomeMessage: '您好，XX 市政务服务中心很高兴为您服务！',
            themeColor: '#1890ff',
            email: 'service@gov.cn',
            phone: '12345',
            serviceTime: '工作时间：周一至周日 9:00-17:00'
        });
        
        const saveSettings = async () => {
            try {
                // 创建一个纯净的对象，避免 reactive 的响应式属性
                const configData = {
                    companyName: settings.companyName,
                    welcomeMessage: settings.welcomeMessage,
                    themeColor: settings.themeColor,
                    email: settings.email,
                    phone: settings.phone,
                    serviceTime: settings.serviceTime
                };
                
                const ok = await window.saveSystemConfig(1, configData);
                if (ok) {
                    ElementPlus.ElMessage.success('设置已保存');
                    nextTick(() => {
                        applyThemeColor(settings.themeColor);
                    });
                } else {
                    ElementPlus.ElMessage.error('保存失败');
                }
            } catch (error) {
                console.error('保存设置失败', error);
                ElementPlus.ElMessage.error('保存失败：' + (error.message || '未知错误'));
            }
        };
        
        const loadSystemSettings = async () => {
            try {
                const config = await window.loadSystemConfig(1);
                if (config) {
                    Object.assign(settings, config);
                    if (config.themeColor) {
                        applyThemeColor(config.themeColor);
                    }
                }
            } catch (error) {
                // 静默失败
            }
        };
        
        const applyThemeColor = (color) => {
            document.documentElement.style.setProperty('--theme-color', color);
            
            const darkerColor = adjustColorBrightness(color, -20);
            document.documentElement.style.setProperty('--theme-gradient-start', color);
            document.documentElement.style.setProperty('--theme-gradient-end', darkerColor);
            
            const sidebar = document.querySelector('.sidebar');
            if (sidebar) {
                sidebar.style.background = `linear-gradient(180deg, ${color} 0%, ${darkerColor} 100%)`;
            }
        };
        
        const adjustColorBrightness = (color, percent) => {
            if (color.startsWith('rgb')) {
                const rgbMatch = color.match(/rgba?\((\d+),\s*(\d+),\s*(\d+)/);
                if (rgbMatch) {
                    let r = parseInt(rgbMatch[1]);
                    let g = parseInt(rgbMatch[2]);
                    let b = parseInt(rgbMatch[3]);
                    
                    r = Math.max(0, Math.min(255, r + (percent * r / 100)));
                    g = Math.max(0, Math.min(255, g + (percent * g / 100)));
                    b = Math.max(0, Math.min(255, b + (percent * b / 100)));
                    
                    return `rgb(${Math.round(r)}, ${Math.round(g)}, ${Math.round(b)})`;
                }
            } else if (color.startsWith('#')) {
                const bigint = parseInt(color.slice(1), 16);
                let r = (bigint >> 16) & 255;
                let g = (bigint >> 8) & 255;
                let b = bigint & 255;
                
                r = Math.max(0, Math.min(255, r + (percent * r / 100)));
                g = Math.max(0, Math.min(255, g + (percent * g / 100)));
                b = Math.max(0, Math.min(255, b + (percent * b / 100)));
                
                return '#' + ((1 << 24) + (Math.round(r) << 16) + (Math.round(g) << 8) + Math.round(b)).toString(16).slice(1);
            }
            return color;
        };
        
        const handleCommand = (command) => {
            if (command === 'logout') {
                handleLogout();
            } else if (command === 'profile') {
                ElementPlus.ElMessage.info('个人设置功能开发中...');
            }
        };
        
        const formatTime = (timeStr) => {
            if (!timeStr) return '';
            try {
                const date = new Date(timeStr);
                const hours = date.getHours().toString().padStart(2, '0');
                const minutes = date.getMinutes().toString().padStart(2, '0');
                const seconds = date.getSeconds().toString().padStart(2, '0');
                return `${hours}:${minutes}:${seconds}`;
            } catch (e) {
                return timeStr;
            }
        };
        
        // 知识库方法
        const loadKnowledgeList = async () => {
            try {
                const result = await window.loadKnowledgeList(
                    knowledgePage.current,
                    knowledgePage.size,
                    knowledgeSearchKeyword.value || '',
                    filterPublishStatus.value ?? null,
                    filterIsTop.value ?? null
                );
                knowledgeList.value = result.records || [];
                knowledgePage.total = result.total || 0;
            } catch (error) {
                console.error('加载知识列表失败', error);
            }
        };
        
        const loadCategories = async () => {
            try {
                const categories = await window.loadCategories();
                categoryList.value = categories || [];
                // 构建分类字典，用于显示
                const map = {};
                categoryList.value.forEach(cat => {
                    map[cat.id] = cat.categoryName;
                });
                categoryMap.value = map;
            } catch (error) {
                console.error('加载分类失败', error);
                categoryList.value = [];
                categoryMap.value = {};
            }
        };
        
        const handleFilterChange = () => {
            knowledgePage.current = 1;
            loadKnowledgeList();
        };
        
        const handleSearch = async () => {
            knowledgePage.current = 1;
            if (knowledgeSearchKeyword.value && knowledgeSearchKeyword.value.trim()) {
                const result = await window.searchKnowledge(
                    knowledgeSearchKeyword.value.trim(),
                    1,
                    knowledgePage.size,
                    filterPublishStatus.value,
                    filterIsTop.value
                );
                knowledgeList.value = result.records;
                knowledgePage.total = result.total;
            } else {
                await loadKnowledgeList();
            }
        };
        
        const handleSizeChange = (size) => {
            knowledgePage.size = size;
            knowledgePage.current = 1;
            if (knowledgeSearchKeyword.value && knowledgeSearchKeyword.value.trim()) {
                handleSearch();
            } else {
                loadKnowledgeList();
            }
        };
        
        const handleCurrentChange = async () => {
            if (knowledgeSearchKeyword.value && knowledgeSearchKeyword.value.trim()) {
                const result = await window.searchKnowledge(
                    knowledgeSearchKeyword.value.trim(),
                    knowledgePage.current,
                    knowledgePage.size,
                    filterPublishStatus.value,
                    filterIsTop.value
                );
                knowledgeList.value = result.records;
                knowledgePage.total = result.total;
            } else {
                loadKnowledgeList();
            }
        };
        
        const handlePrevPage = () => {
            if (knowledgePage.current > 1) {
                knowledgePage.current--;
                handleCurrentChange();
            }
        };
        
        const handleNextPage = () => {
            if (knowledgePage.current < knowledgeTotalPages.value) {
                knowledgePage.current++;
                handleCurrentChange();
            }
        };
        
        const handleJumpPage = () => {
            handleCurrentChange();
        };
        
        const showAddKnowledgeDialog = () => {
            editingKnowledge.value = { publishStatus: 1, tenantId: 1 };
            knowledgeDialogVisible.value = true;
        };
        
        const editKnowledge = (row) => {
            editingKnowledge.value = { ...row, tenantId: 1 };
            knowledgeDialogVisible.value = true;
        };
        
        const deleteKnowledge = async (id) => {
            try {
                await ElementPlus.ElMessageBox.confirm('确定要删除这条知识吗？', '提示', {
                    confirmButtonText: '确定',
                    cancelButtonText: '取消',
                    type: 'warning',
                });
                await window.deleteKnowledge(id);
                ElementPlus.ElMessage.success('删除成功');
                loadKnowledgeList();
            } catch (error) {
                if (error !== 'cancel') {
                    ElementPlus.ElMessage.error('网络错误');
                }
            }
        };
        
        const saveKnowledge = async () => {
            try {
                if (editingKnowledge.value.id) {
                    await window.updateKnowledge({ ...editingKnowledge.value, tenantId: 1 });
                } else {
                    await window.addKnowledge({ ...editingKnowledge.value, tenantId: 1 });
                }
                ElementPlus.ElMessage.success('保存成功');
                knowledgeDialogVisible.value = false;
                loadKnowledgeList();
            } catch (error) {
                ElementPlus.ElMessage.error('保存失败');
            }
        };
        
        const viewKnowledgeDetail = (knowledge) => {
            selectedKnowledge.value = knowledge;
            detailDialogVisible.value = true;
        };
        
        // 对话记录方法
        const loadRecords = async () => {
            loading.value = true;
            try {
                const hasKeyword = recordsSearchKeyword.value && recordsSearchKeyword.value.trim();
                let result;
                
                if (hasKeyword) {
                    result = await window.searchRecords(
                        recordsSearchKeyword.value.trim(),
                        recordsPage.current,
                        recordsPage.size
                    );
                } else {
                    result = await window.loadRecordsList(recordsPage.current, recordsPage.size);
                }
                
                recordsList.value = result.records || [];
                recordsPage.total = result.total || 0;
            } catch (error) {
                console.error('加载记录失败', error);
            } finally {
                loading.value = false;
            }
        };
        
        const handleSearchRecords = () => {
            recordsPage.current = 1;
            loadRecords();
        };
        
        const handleClearSearchRecords = () => {
            recordsSearchKeyword.value = '';
            recordsPage.current = 1;
            loadRecords();
        };
        
        const handleRecordsSizeChange = (size) => {
            recordsPage.size = size;
            loadRecords();
        };
        
        const handleRecordsPrevPage = () => {
            if (recordsPage.current > 1) {
                recordsPage.current--;
                loadRecords();
            }
        };
        
        const handleRecordsNextPage = () => {
            if (recordsPage.current < recordsTotalPages.value) {
                recordsPage.current++;
                loadRecords();
            }
        };
        
        const handleRecordsJumpPage = () => {
            loadRecords();
        };
        
        const viewRecordDetail = async (record) => {
            selectedRecord.value = { ...record };
            recordDetailDialogVisible.value = true;
            
            try {
                const sessionRecords = await window.getSessionDetail(record.sessionId);
                selectedRecord.value.sessionHistory = sessionRecords;
            } catch (error) {
                selectedRecord.value.sessionHistory = [];
            }
        };
        
        const deleteRecord = async (record) => {
            try {
                await ElementPlus.ElMessageBox.confirm('确定要删除整个会话的所有对话记录吗？', '提示', {
                    confirmButtonText: '确定',
                    cancelButtonText: '取消',
                    type: 'warning',
                });
                
                const result = await window.deleteSession(record.sessionId);
                // api.js 里 deleteSession 成功时返回 true
                if (result === true) {
                    ElementPlus.ElMessage.success('删除成功');
                    loadRecords();
                } else {
                    ElementPlus.ElMessage.error('删除失败');
                }
            } catch (error) {
                if (error !== 'cancel') {
                    ElementPlus.ElMessage.error('网络错误');
                }
            }
        };
        
        onMounted(async () => {
            await Promise.all([
                loadDashboard(),
                loadHotQuestions(),
                loadSystemSettings(),
                loadCategories()  // 加载分类字典
            ]);
            
            // 监听分类变更事件（从分类管理页面触发）
            window.addEventListener('storage', (event) => {
                if (event.key === 'category_update') {
                    console.log('【分类变更】检测到分类数据变更，重新加载...');
                    loadCategories();
                }
            });
        });
        
        watch(currentMenu, async (newVal, oldVal) => {
            if (newVal === 'dashboard' && oldVal !== newVal) {
                setTimeout(async () => {
                    await loadHotQuestions();
                }, 300);
            }
        });
        
        // ==================== 导出 ====================
        
        return {
            currentMenu,
            stats,
            hotQuestions,
            settings,
            renderKey,
            userDisplayName,
            categoryMap,
            selectedKnowledge,
            detailDialogVisible,
            getMenuName,
            switchMenu,
            saveSettings,
            handleCommand,
            loadSystemSettings,
            applyThemeColor,
            viewKnowledgeDetail
        };
    }
});

// 注册所有 Element Plus 图标
for (const [key, component] of Object.entries(ElementPlusIconsVue)) {
    app.component(key, component);
}

// 使用 Element Plus（配置中文语言包）
app.use(ElementPlus, {
    locale: typeof ElementPlusLocaleZhCn !== 'undefined' ? ElementPlusLocaleZhCn : undefined
});

// 挂载应用
app.mount('#app');
