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
        const filterCategoryId = ref(undefined);
        const loading = ref(false);
        const selectedRows = ref([]);
        const isInitialized = ref(false); // 防止初始化时触发筛选

        // 表格高度（显式设置，避免 iframe/flex 布局导致表格被压扁）
        const tableHeight = ref(520);
        const computeTableHeight = () => {
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
                let categoryIdParam = filterCategoryId.value;
                if (categoryIdParam === null) {
                    categoryIdParam = -1;
                } else if (categoryIdParam === undefined || categoryIdParam === '') {
                    categoryIdParam = null;
                }
                
                // 将空字符串转换为 null，避免后端类型转换错误
                const publishStatusParam = filterPublishStatus.value === '' ? null : filterPublishStatus.value;
                const isTopParam = filterIsTop.value === '' ? null : filterIsTop.value;
                
                const result = await window.loadKnowledgeList(
                    page.current,
                    page.size,
                    searchKeyword.value || '',
                    publishStatusParam,
                    isTopParam,
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

        // 筛选处理
        let filterTimer = null;
        const handleFilterChange = () => {
            // 防止初始化时触发
            if (!isInitialized.value) return;
            
            if (filterTimer) clearTimeout(filterTimer);
            filterTimer = setTimeout(() => {
                page.current = 1;
                loadKnowledgeList();
            }, 300);
        };

        // 搜索处理
        let searchTimer = null;
        const handleSearch = () => {
            if (searchTimer) clearTimeout(searchTimer);
            searchTimer = setTimeout(() => {
                page.current = 1;
                loadKnowledgeList();
            }, 300);
        };

        // 清除搜索
        const handleClearSearch = () => {
            searchKeyword.value = '';
            page.current = 1;
            loadKnowledgeList();
            ElementPlus.ElMessage.success('已清除搜索条件');
        };
        
        // 分页 - 每页条数变化
        const handleSizeChange = (size) => {
            page.size = size;
            page.current = 1;
            loadKnowledgeList();
        };

        // 分页 - 页码变化
        const handleCurrentChange = async () => {
            loadKnowledgeList();
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
        
        const viewKnowledgeDetail = (row, column, event) => {
            // 如果点击的是复选框列，不触发详情查看
            if (column && column.type === 'selection') {
                return;
            }
            selectedKnowledge.value = row;
            detailVisible.value = true;
        };
        
        // 处理选择变化
        const handleSelectionChange = (selection) => {
            selectedRows.value = selection;
        };
        
        // 批量删除
        const batchDeleteKnowledge = async () => {
            if (selectedRows.value.length === 0) {
                ElementPlus.ElMessage.warning('请先选择要删除的知识');
                return;
            }
            
            try {
                await ElementPlus.ElMessageBox.confirm(
                    `确定要删除选中的 ${selectedRows.value.length} 条知识吗？`,
                    '提示',
                    {
                        confirmButtonText: '确定',
                        cancelButtonText: '取消',
                        type: 'warning',
                    }
                );
                
                const ids = selectedRows.value.map(row => row.id);
                await window.batchDeleteKnowledge(ids);
                ElementPlus.ElMessage.success('批量删除成功');
                selectedRows.value = [];
                page.current = 1;
                loadKnowledgeList();
            } catch (error) {
                if (error !== 'cancel') {
                    ElementPlus.ElMessage.error(error?.message || '批量删除失败');
                }
            }
        };
        
        // 下载模板
        const downloadTemplate = async () => {
            try {
                await window.downloadKnowledgeTemplate();
                ElementPlus.ElMessage.success('模板下载成功');
            } catch (error) {
                ElementPlus.ElMessage.error(error?.message || '模板下载失败');
            }
        };
        
        // 批量导入（由 el-upload 组件触发）
        const importKnowledge = async (uploadFile) => {
            // uploadFile 是 el-upload 组件传递的文件对象
            const file = uploadFile.raw || uploadFile;
            
            if (!file) {
                ElementPlus.ElMessage.error('未选择文件');
                return;
            }
            
            // 验证文件类型
            const fileName = file.name;
            if (!fileName.endsWith('.xlsx') && !fileName.endsWith('.xls')) {
                ElementPlus.ElMessage.error('只支持 Excel 文件格式(.xlsx, .xls)');
                return;
            }
            
            // 验证文件大小（5MB）
            const maxSize = 5 * 1024 * 1024;
            if (file.size > maxSize) {
                ElementPlus.ElMessage.error(`文件大小超过限制（最大5MB），当前文件${(file.size / 1024 / 1024).toFixed(2)}MB`);
                return;
            }
            
            let loadingInstance = null;
            let isCancelled = false;
            
            try {
                // 显示加载动画，锁定整个页面
                loadingInstance = ElementPlus.ElLoading.service({
                    lock: true,
                    text: `正在导入 ${fileName}...`,
                    background: 'rgba(0, 0, 0, 0.8)',
                    customClass: 'import-loading',
                    fullscreen: true
                });
                
                // 禁用所有面板切换和交互
                document.body.style.pointerEvents = 'none';
                
                // 添加取消按钮
                setTimeout(() => {
                    const loadingEl = document.querySelector('.import-loading');
                    if (loadingEl) {
                        loadingEl.style.pointerEvents = 'auto';
                        
                        const cancelBtn = document.createElement('button');
                        cancelBtn.textContent = '取消导入';
                        cancelBtn.style.cssText = `
                            margin-top: 20px;
                            padding: 10px 30px;
                            background: #f56c6c;
                            color: white;
                            border: none;
                            border-radius: 4px;
                            cursor: pointer;
                            font-size: 14px;
                            font-weight: bold;
                        `;
                        cancelBtn.onclick = () => {
                            isCancelled = true;
                            document.body.style.pointerEvents = 'auto';
                            if (loadingInstance) {
                                loadingInstance.close();
                            }
                            ElementPlus.ElMessage.info('已取消导入');
                        };
                        loadingEl.appendChild(cancelBtn);
                    }
                }, 100);
                
                const result = await window.importKnowledgeFromExcel(file);
                
                document.body.style.pointerEvents = 'auto';
                
                if (loadingInstance) {
                    loadingInstance.close();
                }
                
                if (isCancelled) {
                    return;
                }
                
                if (result.failCount > 0) {
                    ElementPlus.ElMessageBox.alert(
                        `导入完成！\n总数: ${result.totalCount} 条\n成功: ${result.successCount} 条\n失败: ${result.failCount} 条` +
                        (result.errorMessages && result.errorMessages.length > 0 ? '\n\n错误详情:\n' + result.errorMessages.join('\n') : ''),
                        '导入结果',
                        {
                            confirmButtonText: '确定',
                            type: result.successCount > 0 ? 'success' : 'warning',
                        }
                    );
                } else {
                    ElementPlus.ElMessage.success(`导入成功！共导入 ${result.successCount} 条知识`);
                }
                
                // 刷新列表
                page.current = 1;
                loadKnowledgeList();
            } catch (error) {
                document.body.style.pointerEvents = 'auto';
                
                if (loadingInstance) {
                    loadingInstance.close();
                }
                if (!isCancelled) {
                    ElementPlus.ElMessage.error(error?.message || '导入失败');
                }
            }
        };
        
        onMounted(async () => {
            computeTableHeight();
            window.addEventListener('resize', computeTableHeight);
            
            // 先加载分类，再加载知识列表（串行加载）
            await loadCategories();
            await loadKnowledgeList();
            
            // 标记初始化完成
            isInitialized.value = true;
        });
        
        return {
            knowledgeList,
            page,
            categoryList,
            searchKeyword,
            filterPublishStatus,
            filterIsTop,
            filterCategoryId,
            loading,
            tableHeight,
            dialogVisible,
            detailVisible,
            editingKnowledge,
            selectedKnowledge,
            categoryMap,
            selectedRows,
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
            viewKnowledgeDetail,
            handleSelectionChange,
            batchDeleteKnowledge,
            downloadTemplate,
            importKnowledge
        };
    }
    });

    // 统一初始化 Element Plus（异步）
    async function setupApp() {
        if (typeof initElementPlus === 'function') {
            await initElementPlus(app);
        } else if (typeof ElementPlus !== 'undefined') {
            app.use(ElementPlus, { 
                locale: typeof ElementPlusLocaleZhCn !== 'undefined' ? ElementPlusLocaleZhCn : undefined 
            });
            
            // 使用本地图标库注册图标
            if (typeof window.ElementPlusIconsVue !== 'undefined') {
                const iconNames = ['Search', 'Delete', 'Plus', 'Edit', 'Close', 'Check', 'Folder', 'Document', 'Upload', 'Download'];
                for (const iconName of iconNames) {
                    if (window.ElementPlusIconsVue[iconName]) {
                        app.component(iconName, window.ElementPlusIconsVue[iconName]);
                    }
                }
            }
        } else {
            renderFatalError('Element Plus 资源未加载。');
            return;
        }
        
        app.mount('#app');
    }

    setupApp().catch(err => {
        console.error('应用初始化失败:', err);
        app.mount('#app');
    });
}
