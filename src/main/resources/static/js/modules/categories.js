/**
 * 分类管理页面 - 独立运行版本
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
        const categoryList = ref([]);
        const loading = ref(false);
        const dialogVisible = ref(false);
        const editingCategory = ref({});

        // 表格高度（显式设置，避免 iframe/flex 布局导致表格被压扁）
        const tableHeight = ref(520);
        const computeTableHeight = () => {
            const reserved = 240;
            tableHeight.value = Math.max(320, window.innerHeight - reserved);
        };
        
        // 加载分类列表
        const loadCategories = async () => {
            loading.value = true;
            try {
                const categories = await window.loadCategories();
                categoryList.value = categories || [];
            } catch (error) {
                console.error(MESSAGE.ERROR.LOAD_CATEGORIES_FAILED, error);
                ElementPlus.ElMessage.error(MESSAGE.ERROR.LOAD_CATEGORIES_FAILED);
            } finally {
                loading.value = false;
            }
        };
        
        // 显示新增对话框
        const showAddCategoryDialog = () => {
            editingCategory.value = { sortOrder: 0 };
            dialogVisible.value = true;
        };
        
        // 编辑分类
        const editCategory = (row) => {
            editingCategory.value = { ...row };
            dialogVisible.value = true;
        };
        
        // 删除分类
        const deleteCategory = async (id) => {
            try {
                await ElementPlus.ElMessageBox.confirm(MESSAGE.WARNING.CONFIRM_DELETE, MESSAGE.WARNING.UNSUPPORTED_OPERATION, {
                    confirmButtonText: '确定',
                    cancelButtonText: '取消',
                    type: 'warning',
                });
                
                await window.deleteCategory(id);
                ElementPlus.ElMessage.success(MESSAGE.SUCCESS.DELETE);
                loadCategories();
                
                // 触发分类变更事件，通知其他页面刷新
                localStorage.setItem('category_update', Date.now().toString());
                localStorage.removeItem('category_update');
            } catch (error) {
                if (error !== 'cancel') {
                    console.error(MESSAGE.ERROR.DELETE_CATEGORY_FAILED, error);
                    ElementPlus.ElMessage.error(MESSAGE.ERROR.DELETE_CATEGORY_FAILED);
                }
            }
        };
        
        // 保存分类
        const saveCategory = async () => {
            try {
                if (!editingCategory.value.categoryName) {
                    ElementPlus.ElMessage.warning(formatMessage(MESSAGE.ERROR.FIELD_REQUIRED, {field: '分类名称'}));
                    return;
                }
                
                if (editingCategory.value.id) {
                    await window.updateCategory(editingCategory.value);
                    ElementPlus.ElMessage.success(MESSAGE.SUCCESS.UPDATE);
                } else {
                    await window.addCategory(editingCategory.value);
                    ElementPlus.ElMessage.success(MESSAGE.SUCCESS.CREATE);
                }
                
                dialogVisible.value = false;
                loadCategories();
                
                // 触发分类变更事件，通知其他页面刷新
                localStorage.setItem('category_update', Date.now().toString());
                localStorage.removeItem('category_update');
            } catch (error) {
                ElementPlus.ElMessage.error(MESSAGE.ERROR.OPERATION_FAILED);
            }
        };
        
        onMounted(() => {
            computeTableHeight();
            window.addEventListener('resize', computeTableHeight);
            loadCategories();
        });
        
        return {
            categoryList,
            loading,
            tableHeight,
            dialogVisible,
            editingCategory,
            showAddCategoryDialog,
            editCategory,
            deleteCategory,
            saveCategory
        };
    }
    });

    // 统一初始化 Element Plus（异步）
    async function setupApp() {
        if (typeof initElementPlus === 'function') {
            await initElementPlus(app);
        } else if (typeof ElementPlus !== 'undefined') {
            // 降级处理
            app.use(ElementPlus, { locale: typeof ElementPlusLocaleZhCn !== 'undefined' ? ElementPlusLocaleZhCn : undefined });
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
