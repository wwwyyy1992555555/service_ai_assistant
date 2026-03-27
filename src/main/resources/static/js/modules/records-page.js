/**
 * 对话记录页面 - 独立运行版本
 */

function renderFatalError(message) {
    const el = document.getElementById('app');
    if (!el) return;
    el.removeAttribute('v-cloak');
    el.innerHTML = `
        <div style="padding: 40px 20px; text-align: center; color: #f56c6c; font-family: Arial;">
            <div style="font-size: 16px; font-weight: 600; margin-bottom: 8px;">页面加载失败</div>
            <div style="font-size: 13px; color: #999;">${message || '请检查网络或刷新重试'}</div>
            <div style="margin-top: 16px;">
                <button onclick="location.reload()" style="padding: 8px 16px; cursor: pointer;">刷新</button>
            </div>
        </div>
    `;
}

if (typeof Vue === 'undefined') {
    renderFatalError('Vue 资源未加载（CDN 失败）。');
} else {
    const { createApp, ref, reactive, onMounted } = Vue;

    const app = createApp({
    setup() {
        // 状态
        const recordsList = ref([]);
        const pageCurrent = ref(1);
        const pageSize = ref(10);
        const pageTotal = ref(0);
        const searchKeyword = ref('');
        const loading = ref(false);

        // 表格高度（显式设置，避免 iframe/flex 布局导致表格被压扁）
        const tableHeight = ref(520);
        const computeTableHeight = () => {
            const reserved = 240;
            tableHeight.value = Math.max(320, window.innerHeight - reserved);
        };
        
        // 对话框
        const detailVisible = ref(false);
        const selectedRecord = ref({});
        
        // 加载记录
        const loadRecords = async () => {
            loading.value = true;
            try {
                const hasKeyword = searchKeyword.value && searchKeyword.value.trim();
                let result;
                
                if (hasKeyword) {
                    result = await window.searchRecords(
                        searchKeyword.value.trim(),
                        pageCurrent.value,
                        pageSize.value
                    );
                } else {
                    result = await window.loadRecordsList(pageCurrent.value, pageSize.value);
                }
                
                // 按会话分组
                const sessionMap = new Map();
                (result.records || []).forEach(record => {
                    if (!sessionMap.has(record.sessionId)) {
                        sessionMap.set(record.sessionId, []);
                    }
                    sessionMap.get(record.sessionId).push(record);
                });
                
                // 更新用户信息
                const processedRecords = result.records ? result.records.map(record => {
                    const sessionRecords = sessionMap.get(record.sessionId);
                    if (sessionRecords && sessionRecords.length > 0) {
                        const latestUserInfo = sessionRecords.slice().reverse().find(r => r.userName || r.userPhone);
                        if (latestUserInfo) {
                            return {
                                ...record,
                                userName: latestUserInfo.userName,
                                userPhone: latestUserInfo.userPhone,
                                userId: latestUserInfo.userId
                            };
                        }
                    }
                    return record;
                }) : [];
                
                recordsList.value = processedRecords;
                pageTotal.value = result.total || 0;
            } catch (error) {
                console.error('加载记录失败', error);
                recordsList.value = [];
                pageTotal.value = 0;
            } finally {
                loading.value = false;
            }
        };
        
        // 搜索
        let searchTimer = null;
        const handleSearch = () => {
            if (searchTimer) clearTimeout(searchTimer);
            searchTimer = setTimeout(() => {
                pageCurrent.value = 1;
                loadRecords();
            }, 500);
        };

        // 清除搜索 - 重置搜索关键词并刷新列表
        const handleClearSearch = () => {
            searchKeyword.value = '';  // 清空搜索框
            pageCurrent.value = 1;  // 重置到第一页
            loadRecords();  // 刷新列表
            ElementPlus.ElMessage.success('已清除搜索条件');
        };
        
        // 分页
        const handleSizeChange = (size) => {
            pageSize.value = size;
            loadRecords();
        };
        
        const handleCurrentChange = () => {
            loadRecords();
        };
        
        // 查看详情（点击行触发）
        const viewRecordDetail = async (record) => {
            selectedRecord.value = { ...record };
            detailVisible.value = true;
            
            try {
                const sessionRecords = await window.getSessionDetail(record.sessionId);
                selectedRecord.value.sessionHistory = sessionRecords;
                
                if (sessionRecords && sessionRecords.length > 0) {
                    const latestRecord = sessionRecords[sessionRecords.length - 1];
                    if (latestRecord.userName || latestRecord.userPhone) {
                        selectedRecord.value.userName = latestRecord.userName;
                        selectedRecord.value.userPhone = latestRecord.userPhone;
                        selectedRecord.value.userId = latestRecord.userId;
                    }
                }
            } catch (error) {
                selectedRecord.value.sessionHistory = [];
            }
        };
        
        // 删除
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
        
        // 格式化时间
        const formatTime = (timeStr) => {
            if (!timeStr) return '';
            try {
                const date = new Date(timeStr);
                const year = date.getFullYear();
                const month = (date.getMonth() + 1).toString().padStart(2, '0');
                const day = date.getDate().toString().padStart(2, '0');
                const hours = date.getHours().toString().padStart(2, '0');
                const minutes = date.getMinutes().toString().padStart(2, '0');
                const seconds = date.getSeconds().toString().padStart(2, '0');
                return `${year}-${month}-${day} ${hours}:${minutes}:${seconds}`;
            } catch (e) {
                return timeStr;
            }
        };
        
        onMounted(() => {
            computeTableHeight();
            window.addEventListener('resize', computeTableHeight);
            loadRecords();
        });
        
        return {
            recordsList,
            pageCurrent,
            pageSize,
            pageTotal,
            searchKeyword,
            loading,
            tableHeight,
            detailVisible,
            selectedRecord,
            loadRecords,
            handleSearch,
            handleClearSearch,
            handleSizeChange,
            handleCurrentChange,
            viewRecordDetail,
            deleteRecord,
            formatTime
        };
    }
    });

    // 注册图标
    if (typeof ElementPlusIconsVue !== 'undefined') {
        for (const [key, component] of Object.entries(ElementPlusIconsVue)) {
            app.component(key, component);
        }
    }

    if (typeof ElementPlus === 'undefined') {
        renderFatalError('Element Plus 资源未加载（CDN 失败）。');
    } else {
        app.use(ElementPlus, { locale: typeof ElementPlusLocaleZhCn !== 'undefined' ? ElementPlusLocaleZhCn : undefined });
        if (document.readyState === 'loading') {
            document.addEventListener('DOMContentLoaded', () => app.mount('#app'));
        } else {
            app.mount('#app');
        }
    }
}
