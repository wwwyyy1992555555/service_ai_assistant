/**
 * 意见反馈管理模块
 */
const { createApp, ref, reactive, onMounted } = Vue;

const app = createApp({
    setup() {
        // 统计数据
        const stats = ref({
            totalFeedbacks: 0,
            pendingCount: 0,
            avgSatisfaction: '0.00'
        });

        // 反馈列表
        const feedbackList = ref([]);
        const loading = ref(false);
        const tableHeight = ref(0);
        
        // 分页
        const pageCurrent = ref(1);
        const pageSize = ref(10);
        const pageTotal = ref(0);

        // 筛选条件
        const filterForm = reactive({
            status: null,
            satisfaction: null,
            keyword: ''  // 新增：用户姓名或手机号搜索
        });

        // 详情对话框
        const detailDialogVisible = ref(false);
        const selectedFeedback = ref({});
        const processing = ref(false);
        const processRemark = ref('');

        // API 基础路径
        const API_BASE_URL = '/api';

        /**
         * 加载统计数据
         */
        async function loadStatistics() {
            try {
                const response = await fetch(`${API_BASE_URL}/consult/feedback/statistics`);
                const result = await response.json();
                
                if (result.code === 200) {
                    stats.value = result.data;
                }
            } catch (error) {
                // 静默失败，不显示错误日志
            }
        }

        /**
         * 加载反馈列表
         */
        async function loadFeedbackList() {
            loading.value = true;
            try {
                // 构建查询参数
                const params = new URLSearchParams();
                params.append('page', pageCurrent.value);
                params.append('size', pageSize.value);
                
                // 添加搜索关键词
                if (filterForm.keyword && filterForm.keyword.trim()) {
                    params.append('keyword', filterForm.keyword.trim());
                }
                
                if (filterForm.status !== null) {
                    params.append('status', filterForm.status);
                }
                if (filterForm.satisfaction !== null) {
                    params.append('satisfaction', filterForm.satisfaction);
                }
                
                const response = await fetch(`${API_BASE_URL}/consult/feedback/list?${params.toString()}`);
                const result = await response.json();
                
                if (result.code === 200) {
                    feedbackList.value = result.data.records || [];
                    pageTotal.value = result.data.total || 0;
                }
            } catch (error) {
                ElementPlus.ElMessage.error('加载失败');
            } finally {
                loading.value = false;
            }
        }
        
        /**
         * 处理分页 - 每页条数变化
         */
        function handleSizeChange(newSize) {
            pageSize.value = newSize;
            pageCurrent.value = 1; // 重置到第一页
            loadFeedbackList();
        }
        
        /**
         * 处理分页 - 页码变化
         */
        function handleCurrentChange(newPage) {
            pageCurrent.value = newPage;
            loadFeedbackList();
        }

        /**
         * 重置筛选
         */
        function resetFilter() {
            filterForm.keyword = '';
            filterForm.status = null;
            filterForm.satisfaction = null;
            loadFeedbackList();
        }

        /**
         * 格式化反馈原因
         */
        function formatFeedbackReasons(reasonsJson) {
            try {
                const reasons = JSON.parse(reasonsJson);
                return Array.isArray(reasons) ? reasons.join(', ') : reasonsJson;
            } catch (e) {
                return reasonsJson;
            }
        }

        /**
         * 解析反馈原因（用于标签显示）
         */
        function parseFeedbackReasons(reasonsJson) {
            try {
                const reasons = JSON.parse(reasonsJson);
                return Array.isArray(reasons) ? reasons : [reasonsJson];
            } catch (e) {
                return [reasonsJson];
            }
        }

        /**
         * 格式化日期
         */
        function formatDate(dateStr) {
            if (!dateStr) return '';
            const date = new Date(dateStr);
            return date.toLocaleString('zh-CN', {
                year: 'numeric',
                month: '2-digit',
                day: '2-digit',
                hour: '2-digit',
                minute: '2-digit'
            });
        }

        /**
         * 查看详情
         */
        function viewDetail(row) {
            selectedFeedback.value = { ...row };
            processRemark.value = '';
            detailDialogVisible.value = true;
        }

        /**
         * 保存编辑
         */
        async function saveFeedbackEdit() {
            processing.value = true;
            try {
                const response = await fetch(`${API_BASE_URL}/consult/feedback/${selectedFeedback.value.id}`, {
                    method: 'PUT',
                    headers: {
                        'Content-Type': 'application/json'
                    },
                    body: JSON.stringify({
                        id: selectedFeedback.value.id,
                        processRemark: selectedFeedback.value.processRemark
                    })
                });

                const result = await response.json();
                
                if (result.code === 200) {
                    ElementPlus.ElMessage.success('保存成功');
                    detailDialogVisible.value = false;
                    loadFeedbackList();
                    loadStatistics();
                } else {
                    ElementPlus.ElMessage.error(result.message || '保存失败');
                }
            } catch (error) {
                ElementPlus.ElMessage.error('保存失败');
            } finally {
                processing.value = false;
            }
        }

        /**
         * 删除反馈
         */
        async function deleteFeedback(id) {
            ElementPlus.ElMessageBox.confirm(
                '确定要删除这条反馈吗？',
                '删除确认',
                {
                    confirmButtonText: '确定',
                    cancelButtonText: '取消',
                    type: 'warning',
                }
            ).then(async () => {
                try {
                    const response = await fetch(`${API_BASE_URL}/consult/feedback/${id}`, {
                        method: 'DELETE'
                    });

                    const result = await response.json();
                    
                    if (result.code === 200) {
                        ElementPlus.ElMessage.success('删除成功');
                        loadFeedbackList();
                        loadStatistics();
                    } else {
                        ElementPlus.ElMessage.error(result.message || '删除失败');
                    }
                } catch (error) {
                    ElementPlus.ElMessage.error('删除失败');
                }
            }).catch(() => {
                // 用户取消
            });
        }

        /**
         * 处理反馈
         */
        async function processFeedback() {
            if (!selectedFeedback.value.processRemark || !selectedFeedback.value.processRemark.trim()) {
                ElementPlus.ElMessage.warning('请输入处理备注');
                return;
            }

            processing.value = true;
            try {
                const response = await fetch(`${API_BASE_URL}/consult/feedback/${selectedFeedback.value.id}`, {
                    method: 'PUT',
                    headers: {
                        'Content-Type': 'application/json'
                    },
                    body: JSON.stringify({
                        id: selectedFeedback.value.id,
                        isProcessed: 1,
                        processRemark: selectedFeedback.value.processRemark,
                        processor: '管理员',
                        processTime: new Date().toISOString()
                    })
                });

                const result = await response.json();
                
                if (result.code === 200) {
                    ElementPlus.ElMessage.success('处理成功');
                    detailDialogVisible.value = false;
                    loadFeedbackList();
                    loadStatistics();
                } else {
                    ElementPlus.ElMessage.error(result.message || '处理失败');
                }
            } catch (error) {
                ElementPlus.ElMessage.error('处理失败');
            } finally {
                processing.value = false;
            }
        }

        /**
         * 计算表格高度
         */
        function calculateTableHeight() {
            const container = document.querySelector('.table-container');
            if (container) {
                const headerOffset = 220; // 表头 + 统计卡片 + 筛选区的高度
                const containerHeight = container.clientHeight;
                // 确保表格有最小高度
                tableHeight.value = Math.max(400, containerHeight - headerOffset);
            }
        }

        onMounted(() => {
            loadStatistics();
            loadFeedbackList();
            // 延迟计算高度，确保 DOM 已渲染
            setTimeout(() => {
                calculateTableHeight();
            }, 100);
            window.addEventListener('resize', calculateTableHeight);
        });

        return {
            stats,
            feedbackList,
            loading,
            tableHeight,
            filterForm,
            detailDialogVisible,
            selectedFeedback,
            processing,
            processRemark,
            pageCurrent,
            pageSize,
            pageTotal,
            loadFeedbackList,
            resetFilter,
            formatFeedbackReasons,
            parseFeedbackReasons,
            formatDate,
            viewDetail,
            saveFeedbackEdit,
            deleteFeedback,
            processFeedback,
            handleSizeChange,
            handleCurrentChange
        };
    }
});

// 注册图标
if (typeof ElementPlusIconsVue !== 'undefined') {
    for (const [key, component] of Object.entries(ElementPlusIconsVue)) {
        app.component(key, component);
    }
}

app.use(ElementPlus, { locale: ElementPlusLocaleZhCn });
app.mount('#app');
