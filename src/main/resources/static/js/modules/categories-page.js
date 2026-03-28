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
                console.error('加载分类失败', error);
                ElementPlus.ElMessage.error('加载分类失败');
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
                await ElementPlus.ElMessageBox.confirm('确定要删除这个分类吗？', '提示', {
                    confirmButtonText: '确定',
                    cancelButtonText: '取消',
                    type: 'warning',
                });
                
                await window.deleteCategory(id);
                ElementPlus.ElMessage.success('删除成功');
                loadCategories();
                
                // 触发分类变更事件，通知其他页面刷新
                localStorage.setItem('category_update', Date.now().toString());
                localStorage.removeItem('category_update');
            } catch (error) {
                if (error !== 'cancel') {
                    console.error('删除失败', error);
                    ElementPlus.ElMessage.error('删除失败');
                }
            }
        };
        
        // 保存分类
        const saveCategory = async () => {
            try {
                if (!editingCategory.value.categoryName) {
                    ElementPlus.ElMessage.warning('请输入分类名称');
                    return;
                }
                
                if (editingCategory.value.id) {
                    await window.updateCategory(editingCategory.value);
                    ElementPlus.ElMessage.success('更新成功');
                } else {
                    await window.addCategory(editingCategory.value);
                    ElementPlus.ElMessage.success('添加成功');
                }
                
                dialogVisible.value = false;
                loadCategories();
                
                // 触发分类变更事件，通知其他页面刷新
                localStorage.setItem('category_update', Date.now().toString());
                localStorage.removeItem('category_update');
            } catch (error) {
                ElementPlus.ElMessage.error('保存失败');
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
