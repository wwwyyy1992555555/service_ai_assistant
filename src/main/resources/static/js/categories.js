/**
 * 分类管理模块
 */
const { createApp, ref, reactive, onMounted } = Vue;

const app = createApp({
    setup() {
        const loading = ref(false);
        const categoryList = ref([]);
        const categoryFormDialogVisible = ref(false);
        const editingCategory = ref({});
        
        // 加载分类列表
        const loadCategories = async () => {
            loading.value = true;
            try {
                const categories = await window.loadCategories();
                categoryList.value = categories;
            } catch (error) {
                console.error('加载分类列表失败:', error);
                ElementPlus.ElMessage.error('加载失败');
            } finally {
                loading.value = false;
            }
        };
        
        // 显示新增分类对话框
        const showAddCategory = () => {
            editingCategory.value = {
                sortOrder: 0,
                status: 1
            };
            categoryFormDialogVisible.value = true;
        };
        
        // 编辑分类
        const editCategory = (row) => {
            editingCategory.value = { ...row };
            categoryFormDialogVisible.value = true;
        };
        
        // 删除分类
        const deleteCategory = async (id) => {
            try {
                await ElementPlus.ElMessageBox.confirm('确定要删除这个分类吗？', '提示', {
                    confirmButtonText: '确定',
                    cancelButtonText: '取消',
                    type: 'warning',
                });
                
                const result = await window.deleteCategory(id);
                if (result.success) {
                    ElementPlus.ElMessage.success('删除成功');
                    loadCategories();
                } else {
                    ElementPlus.ElMessage.error(result.message || '删除失败');
                }
            } catch (error) {
                if (error !== 'cancel') {
                    ElementPlus.ElMessage.error('网络错误');
                }
            }
        };
        
        // 保存分类
        const saveCategory = async () => {
            const result = await window.saveCategory({
                ...editingCategory.value,
                tenantId: 1
            });
            
            if (result.success) {
                ElementPlus.ElMessage.success('保存成功');
                categoryFormDialogVisible.value = false;
                loadCategories();
            } else {
                ElementPlus.ElMessage.error(result.message || '保存失败');
            }
        };
        
        // 返回管理后台
        const goBack = () => {
            window.location.href = '/admin.html';
        };
        
        // 初始化
        onMounted(() => {
            loadCategories();
        });
        
        return {
            loading,
            categoryList,
            categoryFormDialogVisible,
            editingCategory,
            loadCategories,
            showAddCategory,
            editCategory,
            deleteCategory,
            saveCategory,
            goBack
        };
    }
});

// 注册所有 Element Plus 图标
for (const [key, component] of Object.entries(ElementPlusIconsVue)) {
    app.component(key, component);
}

// 使用 Element Plus
app.use(ElementPlus);

// 挂载应用
app.mount('#app');
