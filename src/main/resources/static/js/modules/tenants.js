/**
 * 租户管理页面逻辑
 */

// ==================== 权限检查（仅超级管理员可访问）====================
const currentUser = JSON.parse(localStorage.getItem('user') || '{}');

// 只有超级管理员（tenantId === 0）才能访问
if (currentUser.tenantId !== 0) {
    ElementPlus.ElMessage.error('您没有权限访问此页面');
    setTimeout(() => {
        window.top.location.href = '/admin.html';
    }, 1500);
}

const { createApp, onMounted, onUnmounted, nextTick } = Vue;

const app = createApp({
    setup() {
        const tenantsList = Vue.ref([]);
        const loading = Vue.ref(false);
        const searchKeyword = Vue.ref('');
        const filterStatus = Vue.ref(null); // 状态筛选
        const filterIndustryType = Vue.ref(null); // 行业类型筛选
        
        // 行业类型列表
        const industryTypeList = Vue.ref([]);
        
        // 分页相关
        const page = Vue.reactive({
            current: 1,
            size: 10,
            total: 0
        });
        
        // 对话框相关状态
        const dialogVisible = Vue.ref(false);
        const submitting = Vue.ref(false);
        const formRef = Vue.ref(null);
        const isEditMode = Vue.ref(false); // 是否为编辑模式
        
        // 表单数据
        const formData = Vue.reactive({
            tenantCode: '',
            tenantName: '',
            industryType: null,
            contactPerson: '',
            contactPhone: '',
            expireTime: '',
            remark: ''
        });
        
        // 表单验证规则
        const formRules = {
            tenantCode: [
                { required: true, message: '请输入租户编码', trigger: 'blur' },
                { min: 2, max: 50, message: '租户编码长度在 2 到 50 个字符', trigger: 'blur' },
                {
                    pattern: /^[a-zA-Z0-9_-]+$/,
                    message: '租户编码只能包含字母、数字、下划线和横线',
                    trigger: 'blur'
                }
            ],
            tenantName: [
                { required: true, message: '请输入租户名称', trigger: 'blur' },
                { min: 2, max: 100, message: '租户名称长度在 2 到 100 个字符', trigger: 'blur' }
            ],
            industryType: [
                { required: true, message: '请选择行业类型', trigger: 'change' }
            ],
            expireTime: [
                { required: true, message: '请选择到期时间', trigger: 'change' }
            ]
        };
        
        // 加载行业类型列表
        const loadIndustryTypes = async () => {
            try {
                const result = await window.api.industryType.getList();
                industryTypeList.value = result || [];
            } catch (error) {
                console.error('加载行业类型失败:', error);
            }
        };
        
        // 加载租户列表
        const loadTenants = async () => {
            loading.value = true;
            try {
                const result = await window.api.tenant.getList(page.current, page.size, searchKeyword.value, filterStatus.value, filterIndustryType.value);
                tenantsList.value = result.records || [];
                page.total = result.total || 0;
            } catch (error) {
                console.error('加载租户列表失败:', error);
                ElementPlus.ElMessage.error(error.message || '网络错误，请稍后重试');
            } finally {
                loading.value = false;
            }
        };
        
        // 搜索处理
        const handleSearch = () => {
            page.current = 1;
            loadTenants();
        };
        
        const handleClearSearch = () => {
            searchKeyword.value = '';
            filterStatus.value = null;
            filterIndustryType.value = null;
            page.current = 1;
            loadTenants();
        };
        
        // 筛选变化处理
        const handleFilterChange = () => {
            page.current = 1;
            loadTenants();
        };
        
        // 分页处理
        const handleSizeChange = (size) => {
            page.size = size;
            page.current = 1;
            loadTenants();
        };
        
        const handleCurrentChange = (current) => {
            page.current = current;
            loadTenants();
        };
        
        // 新建租户
        const handleCreateTenant = () => {
            isEditMode.value = false;
            // 默认到期时间为 1 年后
            const oneYearLater = new Date();
            oneYearLater.setFullYear(oneYearLater.getFullYear() + 1);
            const expireTimeStr = oneYearLater.toISOString().replace('T', ' ').substring(0, 19);
            
            Object.assign(formData, {
                tenantCode: '',
                tenantName: '',
                industryType: null,
                contactPerson: '',
                contactPhone: '',
                expireTime: expireTimeStr,
                remark: ''
            });
            dialogVisible.value = true;
        };
        
        // 编辑租户
        const handleEditTenant = (row) => {
            isEditMode.value = true;
            Object.assign(formData, row);
            dialogVisible.value = true;
        };
        
        // 提交表单（支持新建和编辑）
        const handleSubmit = async () => {
            if (!formRef.value) return;
            
            try {
                await formRef.value.validate();
                
                submitting.value = true;
                if (isEditMode.value && formData.id) {
                    // 编辑模式：调用更新接口
                    await window.api.tenant.update(formData);
                    ElementPlus.ElMessage.success('更新成功');
                } else {
                    // 新建模式
                    await window.api.tenant.create(formData);
                    ElementPlus.ElMessage.success('创建成功');
                }
                dialogVisible.value = false;
                loadTenants();
            } catch (error) {
                if (error.message && error.message.includes('验证')) {
                    return;
                }
                console.error('保存租户失败:', error);
                ElementPlus.ElMessage.error(error.message || '保存失败，请稍后重试');
            } finally {
                submitting.value = false;
            }
        };
        
        // 切换租户状态
        const handleToggleStatus = async (row) => {
            const newStatus = row.status === 1 ? 0 : 1;
            const actionText = newStatus === 1 ? '启用' : '禁用';
            
            try {
                await ElementPlus.ElMessageBox.confirm(
                    `确定要${actionText}租户"${row.tenantName}"吗？`,
                    '提示',
                    {
                        confirmButtonText: '确定',
                        cancelButtonText: '取消',
                        type: 'warning'
                    }
                );
                
                await window.api.tenant.updateStatus(row.id, newStatus);
                ElementPlus.ElMessage.success(`${actionText}成功`);
                loadTenants();
            } catch (error) {
                if (error !== 'cancel') {
                    console.error(`${actionText}租户失败:`, error);
                    ElementPlus.ElMessage.error(error.message || `${actionText}失败，请稍后重试`);
                }
            }
        };
        
        // 获取行业类型名称
        const getIndustryTypeName = (typeId) => {
            if (!typeId) return '-';
            const type = industryTypeList.value.find(item => item.id === typeId);
            return type ? type.typeName : '-';
        };
        
        // 判断是否已过期
        const isExpired = (expireTime) => {
            if (!expireTime) return false;
            const expireDate = new Date(expireTime);
            const now = new Date();
            return expireDate < now;
        };
        
        // 格式化日期
        const formatDate = (dateStr) => {
            if (!dateStr) return '-';
            const date = new Date(dateStr);
            const year = date.getFullYear();
            const month = String(date.getMonth() + 1).padStart(2, '0');
            const day = String(date.getDate()).padStart(2, '0');
            const hours = String(date.getHours()).padStart(2, '0');
            const minutes = String(date.getMinutes()).padStart(2, '0');
            const seconds = String(date.getSeconds()).padStart(2, '0');
            return `${year}-${month}-${day} ${hours}:${minutes}:${seconds}`;
        };
        
        // 组件挂载时加载数据
        onMounted(() => {
            loadIndustryTypes();
            loadTenants();
        });
        
        return {
            tenantsList,
            loading,
            searchKeyword,
            filterStatus,
            filterIndustryType,
            industryTypeList,
            page,
            dialogVisible,
            submitting,
            formRef,
            formData,
            formRules,
            isEditMode,
            handleSearch,
            handleClearSearch,
            handleFilterChange,
            handleSizeChange,
            handleCurrentChange,
            handleCreateTenant,
            handleEditTenant,
            handleSubmit,
            handleToggleStatus,
            getIndustryTypeName,
            isExpired,
            formatDate
        };
    }
});

// 使用公共的 Element Plus 配置
initElementPlus(app);

app.mount('#app');
