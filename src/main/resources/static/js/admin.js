 /**
 * 管理后台主应用
 */

// 检查登录状态
const token = localStorage.getItem('token');
const userStr = localStorage.getItem('user');

if (!token || !userStr) {
    // 未登录，跳转到登录页
    window.location.href = '/login.html';
}

const user = userStr ? JSON.parse(userStr) : null;

const { createApp, ref, reactive, onMounted, computed } = Vue;

const app = createApp({
    setup() {
        // 状态定义
        const currentMenu = ref('dashboard');
        const searchKeyword = ref('');
        const searchQuestion = ref('');
        const knowledgeDialogVisible = ref(false);
        const editingKnowledge = ref({});
        const loading = ref(false);
        const renderKey = ref(0);
        const dataLoaded = ref(false); // 数据是否加载完成
        
        // 知识详情对话框
        const detailDialogVisible = ref(false);
        const selectedKnowledge = ref({});
        
        // 统计数据
        const stats = reactive({
            todayConsultations: 0,
            consultationGrowth: 0,
            solveRate: 0,
            solveRateGrowth: 0,
            avgSatisfaction: 0,
            knowledgeCount: 0,
            knowledgeCategories: 0
        });
        
        // 热门问题
        const hotQuestions = ref([]);
        
        // 知识列表
        const knowledgeList = ref([]);
        const knowledgePage = reactive({
            current: 1,
            size: 10,
            total: 0
        });
        
        // 分类列表
        const categoryList = ref([]);
        
        // 对话记录
        const recordsList = ref([]);
        const recordsPage = reactive({
            current: 1,
            size: 10,
            total: 0
        });
        const recordsJumpPage = ref(1);
        
        // 选中的对话记录
        const selectedRecord = ref({});
        const recordDetailDialogVisible = ref(false);
        
        // 计算对话记录总页数
        const recordsTotalPages = computed(() => {
            return Math.ceil(recordsPage.total / recordsPage.size) || 1;
        });
        
        // 系统设置
        const settings = reactive({
            companyName: 'XX 市政务服务中心',
            welcomeMessage: '您好，XX 市政务服务中心很高兴为您服务！',
            themeColor: '#1890ff',
            email: 'service@gov.cn',
            phone: '12345'
        });
        
        const jumpPage = ref(1);
        
        // 计算总页数
        const totalPages = computed(() => {
            return Math.ceil(knowledgePage.total / knowledgePage.size) || 1;
        });
        
        // 分类字典
        const categoryMap = {
            1: '办事服务',
            2: '社保医保',
            3: '户籍办理',
            4: '居住证办理',
            5: '公积金',
            6: '工商登记',
            7: '税务服务',
            8: '医疗卫生'
        };
        
        // 获取菜单名称
        const getMenuName = () => {
            const names = {
                'dashboard': '数据看板',
                'knowledge': '知识库管理',
                'records': '对话记录',
                'settings': '系统设置'
            };
            return names[currentMenu.value];
        };
        
        // 加载数据
        const loadDashboard = async () => {
            const data = await window.loadDashboard();
            if (data) {
                Object.assign(stats, data);
            }
        };
        
        const loadHotQuestions = async () => {
            const data = await window.loadHotQuestions(10);
            if (data && data.length > 0) {
                hotQuestions.value.splice(0, hotQuestions.value.length, ...data);
                renderKey.value++;
            }
        };
        
        const loadKnowledgeList = async () => {
            const result = await window.loadKnowledgeList(knowledgePage.current, knowledgePage.size);
            knowledgeList.value = result.records;
            knowledgePage.total = result.total;
        };
        
        const loadRecords = async () => {
            loading.value = true;
            try {
                const result = await window.loadRecordsPage(recordsPage.current, recordsPage.size, searchQuestion.value);
                recordsList.value = result.records;
                recordsPage.total = result.total;
            } finally {
                loading.value = false;
            }
        };
        
        // 加载分类列表
        const loadCategories = async () => {
            try {
                const categories = await window.loadCategories();
                categoryList.value = categories || [];
            } catch (error) {
                categoryList.value = [];
            }
        };
        
        // 知识管理操作
        const showAddKnowledgeDialog = () => {
            editingKnowledge.value = { 
                publishStatus: 1,
                tenantId: 1
            };
            knowledgeDialogVisible.value = true;
        };
        
        const editKnowledge = (row) => {
            editingKnowledge.value = { 
                ...row,
                tenantId: 1
            };
            knowledgeDialogVisible.value = true;
        };
        
        const deleteKnowledge = async (id) => {
            try {
                await ElementPlus.ElMessageBox.confirm('确定要删除这条知识吗？', '提示', {
                    confirmButtonText: '确定',
                    cancelButtonText: '取消',
                    type: 'warning',
                });
                
                const result = await window.deleteKnowledge(id);
                if (result.success) {
                    ElementPlus.ElMessage.success('删除成功');
                    loadKnowledgeList();
                } else {
                    ElementPlus.ElMessage.error(result.message || '删除失败');
                }
            } catch (error) {
                if (error !== 'cancel') {
                    ElementPlus.ElMessage.error('网络错误');
                }
            }
        };
        
        const saveKnowledge = async () => {
            const result = await window.saveKnowledge({
                ...editingKnowledge.value,
                tenantId: 1
            });
            
            if (result.success) {
                ElementPlus.ElMessage.success('保存成功');
                knowledgeDialogVisible.value = false;
                loadKnowledgeList();
            } else {
                ElementPlus.ElMessage.error(result.message || '保存失败');
            }
        };
        
        // 事件处理
        const handleSearchRecords = () => {
            recordsPage.current = 1;
            loadRecords();
        };
        
        const handleClearSearchRecords = () => {
            searchQuestion.value = '';
            recordsPage.current = 1;
            loadRecords();
        };
        
        const handleRecordsSizeChange = (val) => {
            recordsPage.size = val;
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
            if (recordsJumpPage.value >= 1 && recordsJumpPage.value <= recordsTotalPages.value) {
                recordsPage.current = recordsJumpPage.value;
                loadRecords();
            }
        };
        
        // 查看对话记录详情
        const viewRecordDetail = async (record) => {
            selectedRecord.value = { ...record };
            recordDetailDialogVisible.value = true;
            
            // 加载完整的会话对话历史
            try {
                const sessionRecords = await window.getSessionDetail(record.sessionId);
                selectedRecord.value.sessionHistory = sessionRecords;
            } catch (error) {
                selectedRecord.value.sessionHistory = [];
            }
        };
        
        // 格式化时间
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
        
        // 删除对话记录（按会话删除）
        const deleteRecord = async (record) => {
            try {
                await ElementPlus.ElMessageBox.confirm('确定要删除整个会话的所有对话记录吗？', '提示', {
                    confirmButtonText: '确定',
                    cancelButtonText: '取消',
                    type: 'warning',
                });
                
                const result = await window.deleteSession(record.sessionId);
                if (result.success) {
                    ElementPlus.ElMessage.success('删除成功');
                    loadRecords();
                } else {
                    ElementPlus.ElMessage.error(result.message || '删除失败');
                }
            } catch (error) {
                if (error !== 'cancel') {
                    ElementPlus.ElMessage.error('网络错误');
                }
            }
        };
        
        const handleSearch = async () => {
            knowledgePage.current = 1;
            if (searchKeyword.value && searchKeyword.value.trim()) {
                // 执行搜索
                const result = await window.searchKnowledge(searchKeyword.value.trim(), 1, knowledgePage.size);
                knowledgeList.value = result.records;
                knowledgePage.total = result.total;
                ElementPlus.ElMessage.success(`找到 ${result.total} 条相关记录`);
            } else {
                // 清空搜索，重新加载
                await loadKnowledgeList();
            }
        };
        
        // 搜索后分页处理
        const handleSearchCurrentChange = async (val) => {
            knowledgePage.current = val;
            if (searchKeyword.value && searchKeyword.value.trim()) {
                const result = await window.searchKnowledge(searchKeyword.value.trim(), val, knowledgePage.size);
                knowledgeList.value = result.records;
                knowledgePage.total = result.total;
            }
        };
        
        const handleSizeChange = (size) => {
            knowledgePage.size = size;
            knowledgePage.current = 1;
            // 如果有搜索关键词，使用搜索
            if (searchKeyword.value && searchKeyword.value.trim()) {
                handleSearch();
            } else {
                loadKnowledgeList();
            }
        };
        
        const handleCurrentChange = async () => {
            // 如果有搜索关键词，使用搜索分页
            if (searchKeyword.value && searchKeyword.value.trim()) {
                await handleSearchCurrentChange(knowledgePage.current);
            } else {
                await loadKnowledgeList();
            }
        };
        
        // 分页处理函数
        const handlePrevPage = () => {
            if (knowledgePage.current > 1) {
                knowledgePage.current--;
                // 如果有搜索关键词，使用搜索分页
                if (searchKeyword.value && searchKeyword.value.trim()) {
                    handleSearchCurrentChange(knowledgePage.current);
                } else {
                    loadKnowledgeList();
                }
            }
        };
        
        const handleNextPage = () => {
            if (knowledgePage.current < totalPages.value) {
                knowledgePage.current++;
                // 如果有搜索关键词，使用搜索分页
                if (searchKeyword.value && searchKeyword.value.trim()) {
                    handleSearchCurrentChange(knowledgePage.current);
                } else {
                    loadKnowledgeList();
                }
            }
        };
        
        const handleJumpPage = () => {
            const page = Number(jumpPage.value);
            if (isNaN(page) || page < 1) {
                ElementPlus.ElMessage.warning('请输入有效的页码');
                return;
            }
            if (page > totalPages.value) {
                ElementPlus.ElMessage.warning(`最大页数为 ${totalPages.value}`);
                return;
            }
            knowledgePage.current = page;
            // 如果有搜索关键词，使用搜索分页
            if (searchKeyword.value && searchKeyword.value.trim()) {
                handleSearchCurrentChange(knowledgePage.current);
            } else {
                loadKnowledgeList();
            }
        };
        
        const viewKnowledgeDetail = (knowledge) => {
            selectedKnowledge.value = knowledge;
            detailDialogVisible.value = true;
        };
        
        const handleCommand = (command) => {
            if (command === 'logout') {
                handleLogout();
            } else if (command === 'profile') {
                ElementPlus.ElMessage.info('个人设置功能开发中...');
            }
        };
        
        // 退出登录
        const handleLogout = async () => {
            try {
                await ElementPlus.ElMessageBox.confirm('确定要退出登录吗？', '提示', {
                    confirmButtonText: '确定',
                    cancelButtonText: '取消',
                    type: 'warning'
                });
                
                // 清除本地存储
                localStorage.removeItem('token');
                localStorage.removeItem('user');
                
                // 调用后端登出接口
                if (user && user.id) {
                    await fetch(`/api/auth/logout?userId=${user.id}`, {
                        method: 'POST'
                    });
                }
                
                ElementPlus.ElMessage.success('已退出登录');
                
                // 跳转到登录页
                setTimeout(() => {
                    window.location.href = '/login.html';
                }, 500);
            } catch (error) {
                if (error !== 'cancel') {
                    // 用户取消退出
                }
            }
        };
        
        const saveSettings = async () => {
            try {
                const result = await window.saveSystemConfig(1, settings);
                
                if (result.success) {
                    ElementPlus.ElMessage.success('设置已保存');
                    
                    // 立即应用主题颜色
                    Vue.nextTick(() => {
                        applyThemeColor(settings.themeColor);
                    });
                } else {
                    ElementPlus.ElMessage.error(result.message || '保存失败');
                }
            } catch (error) {
                ElementPlus.ElMessage.error('保存失败');
            }
        };
        
        // 加载系统设置
        const loadSystemSettings = async () => {
            try {
                const config = await window.loadSystemConfig(1);
                if (config) {
                    Object.assign(settings, config);
                    
                    // 应用主题颜色（无论是否是默认值都应用）
                    if (config.themeColor) {
                        applyThemeColor(config.themeColor);
                    }
                }
            } catch (error) {
                // 静默失败
            }
        };
        
        // 应用主题颜色
        const applyThemeColor = (color) => {
            // 设置到根元素
            document.documentElement.style.setProperty('--theme-color', color);
            
            // 计算渐变色（深色版本）
            const darkerColor = adjustColorBrightness(color, -20);
            document.documentElement.style.setProperty('--theme-gradient-start', color);
            document.documentElement.style.setProperty('--theme-gradient-end', darkerColor);
            
            // 强制刷新侧边栏的背景
            const sidebar = document.querySelector('.sidebar');
            if (sidebar) {
                sidebar.style.background = `linear-gradient(180deg, ${color} 0%, ${darkerColor} 100%)`;
            }
        };
        
        // 调整颜色亮度（用于生成渐变色）
        const adjustColorBrightness = (color, percent) => {
            // 处理 RGB 格式
            if (color.startsWith('rgb')) {
                // 解析 rgb(r, g, b) 格式
                const rgbMatch = color.match(/rgba?\((\d+),\s*(\d+),\s*(\d+)/);
                if (!rgbMatch) {
                    return color;
                }
                let r = parseInt(rgbMatch[1]);
                let g = parseInt(rgbMatch[2]);
                let b = parseInt(rgbMatch[3]);
                
                // 调整亮度
                r = Math.max(0, Math.min(255, r + percent));
                g = Math.max(0, Math.min(255, g + percent));
                b = Math.max(0, Math.min(255, b + percent));
                
                return `rgb(${r}, ${g}, ${b})`;
            }
            
            // 处理十六进制格式
            let hex = color.replace(/^#/, '');
            
            // 解析 RGB 值
            let r = parseInt(hex.substring(0, 2), 16);
            let g = parseInt(hex.substring(2, 4), 16);
            let b = parseInt(hex.substring(4, 6), 16);
            
            // 处理短格式十六进制（如 #fff）
            if (hex.length === 3) {
                r = parseInt(hex.substring(0, 1) + hex.substring(0, 1), 16);
                g = parseInt(hex.substring(1, 2) + hex.substring(1, 2), 16);
                b = parseInt(hex.substring(2, 3) + hex.substring(2, 3), 16);
            }
            
            // 调整亮度
            r = Math.max(0, Math.min(255, r + percent));
            g = Math.max(0, Math.min(255, g + percent));
            b = Math.max(0, Math.min(255, b + percent));
            
            // 转回十六进制
            return '#' + 
                r.toString(16).padStart(2, '0') + 
                g.toString(16).padStart(2, '0') + 
                b.toString(16).padStart(2, '0');
        };
        
        // 初始化
        onMounted(async () => {
            // 先应用默认主题颜色
            applyThemeColor('#1890ff');
            
            // 并行加载核心数据和分类列表
            await Promise.all([
                loadDashboard(),
                loadHotQuestions(),
                loadKnowledgeList(),
                loadRecords(),
                loadCategories(),
                loadSystemSettings()
            ]);
        });
        
        return {
            currentMenu,
            searchQuestion,
            searchKeyword,
            knowledgeDialogVisible,
            editingKnowledge,
            stats,
            hotQuestions,
            knowledgeList,
            categoryList,
            knowledgePage,
            recordsList,
            recordsPage,
            recordsJumpPage,
            recordsTotalPages,
            selectedRecord,
            recordDetailDialogVisible,
            settings,
            loading,
            renderKey,
            detailDialogVisible,
            selectedKnowledge,
            categoryMap,
            totalPages,
            jumpPage,
            getMenuName,
            showAddKnowledgeDialog,
            editKnowledge,
            deleteKnowledge,
            saveKnowledge,
            saveSettings,
            handleSizeChange,
            handleCurrentChange,
            handlePrevPage,
            handleNextPage,
            handleJumpPage,
            handleRecordsSizeChange,
            handleRecordsPrevPage,
            handleRecordsNextPage,
            handleRecordsJumpPage,
            handleSearchRecords,
            handleClearSearchRecords,
            handleSearch,
            viewKnowledgeDetail,
            viewRecordDetail,
            formatTime,
            deleteRecord,
            handleCommand,
            loadRecords,
            loadKnowledgeList,
            loadCategories,
            loadSystemSettings,
            applyThemeColor
        };
    }
});

// 注册所有 Element Plus 图标
for (const [key, component] of Object.entries(ElementPlusIconsVue)) {
    app.component(key, component);
}

// 使用 Element Plus 并配置中文语言包
app.use(ElementPlus, {
    locale: ElementPlusLocaleZhCn,
    el: {
        pagination: {
            goto: '跳至',
            pagesize: '条/页',
            total: '共 {total} 条',
            pageClassifier: '页'
        },
        table: {
            emptyText: '暂无数据'
        },
        colorPicker: {
            confirm: '确定',
            clear: '清空'
        }
    }
});

// 挂载应用并显示页面
app.mount('#app');
