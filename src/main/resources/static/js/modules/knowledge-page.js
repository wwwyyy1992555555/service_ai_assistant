/**
 * 知识库管理页面 - 独立运行版本
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
        const knowledgeList = ref([]);
        const page = reactive({ current: 1, size: 10, total: 0 });
        const categoryList = ref([]);
        const searchKeyword = ref('');
        const filterPublishStatus = ref(undefined);
        const filterIsTop = ref(undefined);
        const filterCategoryId = ref(undefined);  // 新增：分类筛选
        const loading = ref(false);

        // 表格高度（显式设置，避免 iframe/flex 布局导致表格被压扁）
        const tableHeight = ref(520);
        const computeTableHeight = () => {
            // 预留：顶部工具栏 + 分页 + padding 等
            const reserved = 260;
            tableHeight.value = Math.max(320, window.innerHeight - reserved);
        };
        
        // 对话框
        const dialogVisible = ref(false);
        const detailVisible = ref(false);
        const editingKnowledge = ref({});
        const selectedKnowledge = ref({});
        
        // 分类字典（从后端加载）
        const categoryMap = ref({});
        
        // 加载知识列表（统一入口，支持搜索 + 筛选）
        const loadKnowledgeList = async () => {
            loading.value = true;
            try {
                // 处理分类筛选：undefined=全部，null=其他（无分类），其他值=指定分类
                let categoryIdParam = filterCategoryId.value;
                if (categoryIdParam === null) {
                    categoryIdParam = -1;  // 特殊值：表示筛选无分类的数据
                } else if (categoryIdParam === undefined) {
                    categoryIdParam = null;  // 不筛选
                }
                
                const result = await window.loadKnowledgeList(
                    page.current,
                    page.size,
                    searchKeyword.value || '',
                    filterPublishStatus.value ?? null,
                    filterIsTop.value ?? null,
                    categoryIdParam
                );
                knowledgeList.value = result.records || [];
                page.total = result.total || 0;
                if ((result.total ?? 0) > 0 && knowledgeList.value.length === 0) {
                    ElementPlus.ElMessage.warning('后端返回 total>0 但 records 为空，请检查分页字段映射（records/total/current/size）');
                }
            } catch (error) {
                ElementPlus.ElMessage.error(error?.message ? `加载失败：${error.message}` : '加载失败，请检查后端服务是否正常');
                knowledgeList.value = [];
                page.total = 0;
            } finally {
                loading.value = false;
            }
        };

        // 加载分类
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
                categoryList.value = [];
                categoryMap.value = {};
            }
        };

        // 筛选处理 - 统一使用 loadKnowledgeList
        let filterTimer = null;
        const handleFilterChange = () => {
            if (filterTimer) clearTimeout(filterTimer);
            filterTimer = setTimeout(() => {
                page.current = 1;  // 重置到第一页
                loadKnowledgeList();  // 使用统一入口，保持搜索关键词
            }, 300);
        };

        // 搜索处理 - 统一使用 loadKnowledgeList
        let searchTimer = null;
        const handleSearch = () => {
            if (searchTimer) clearTimeout(searchTimer);
            searchTimer = setTimeout(() => {
                page.current = 1;  // 重置到第一页
                loadKnowledgeList();  // 使用统一入口，保持筛选条件
            }, 300);
        };

        // 清除搜索 - 重置搜索关键词并刷新列表
        const handleClearSearch = () => {
            searchKeyword.value = '';  // 清空搜索框
            page.current = 1;  // 重置到第一页
            // 注意：不清空筛选条件，只清除搜索关键词
            loadKnowledgeList();
            ElementPlus.ElMessage.success('已清除搜索条件');
        };
        
        // 分页 - 每页条数变化
        const handleSizeChange = (size) => {
            page.size = size;
            page.current = 1;  // 重置到第一页
            loadKnowledgeList();  // 使用统一入口，保持搜索和筛选条件
        };

        // 分页 - 页码变化
        const handleCurrentChange = async () => {
            loadKnowledgeList();  // 使用统一入口，保持搜索和筛选条件
        };
        
        // 操作
        const showAddKnowledgeDialog = () => {
            editingKnowledge.value = { publishStatus: 1, isTop: 0, tenantId: 1 };
            dialogVisible.value = true;
        };

        const editKnowledge = (row) => {
            editingKnowledge.value = { ...row, tenantId: 1 };
            dialogVisible.value = true;
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
                dialogVisible.value = false;
                loadKnowledgeList();
            } catch (error) {
                ElementPlus.ElMessage.error('保存失败');
            }
        };
        
        const viewKnowledgeDetail = (knowledge) => {
            selectedKnowledge.value = knowledge;
            detailVisible.value = true;
        };
        
        onMounted(() => {
            computeTableHeight();
            window.addEventListener('resize', computeTableHeight);
            Promise.all([loadKnowledgeList(), loadCategories()]);
        });
        
        return {
            knowledgeList,
            page,
            categoryList,
            searchKeyword,
            filterPublishStatus,
            filterIsTop,
            filterCategoryId,  // 新增：导出分类筛选
            loading,
            tableHeight,
            dialogVisible,
            detailVisible,
            editingKnowledge,
            selectedKnowledge,
            categoryMap,
            loadKnowledgeList,
            loadCategories,
            handleFilterChange,
            handleSearch,
            handleClearSearch,
            handleSizeChange,
            handleCurrentChange,
            showAddKnowledgeDialog,
            editKnowledge,
            deleteKnowledge,
            saveKnowledge,
            viewKnowledgeDetail
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
        app.use(ElementPlus, {
            locale: typeof ElementPlusLocaleZhCn !== 'undefined' ? ElementPlusLocaleZhCn : undefined
        });
        if (document.readyState === 'loading') {
            document.addEventListener('DOMContentLoaded', () => app.mount('#app'));
        } else {
            app.mount('#app');
        }
    }
}
