/**
 * 用户管理页面逻辑
 */

// ==================== 权限检查 ====================
const userStr = localStorage.getItem('user');
if (!userStr) {
    ElementPlus.ElMessage.error('请先登录');
    window.location.href = '/login.html';
}

const currentUser = JSON.parse(userStr);

const { createApp, onMounted, onUnmounted, nextTick } = Vue;

const app = createApp({
    setup() {
        // 是否为超级管理员
        const isSuperAdmin = Vue.ref(userStr && currentUser.tenantId === 0);
        
        // 当前登录用户的角色等级（用于控制可创建的用户等级）
        const roleLevelValue = currentUser.roleLevel !== undefined ? Number(currentUser.roleLevel) : 2;
        console.log('【用户等级调试】currentUser.roleLevel:', currentUser.roleLevel, '转换为数字:', roleLevelValue);
        const currentUserRoleLevel = Vue.ref(roleLevelValue);
        
        const usersList = Vue.ref([]);
        const loading = Vue.ref(false);
        const searchKeyword = Vue.ref('');
        
        // 分页相关
        const page = Vue.reactive({
            current: 1,
            size: 10,
            total: 0
        });
        
        // 对话框相关状态
        const dialogVisible = Vue.ref(false);
        const dialogType = Vue.ref('create'); // 'create' 或 'edit'
        const editingUser = Vue.ref(null);
        const submitting = Vue.ref(false);
        const formRef = Vue.ref(null);
        
        // 查看用户详情对话框相关状态
        const viewDialogVisible = Vue.ref(false);
        const viewUser = Vue.ref(null);
        
        // 重置密码对话框相关状态
        const resetPasswordDialogVisible = Vue.ref(false);
        const resettingPassword = Vue.ref(false);
        const resetPasswordFormRef = Vue.ref(null);
        
        // 租户搜索相关
        const tenantOptions = Vue.ref([]);
        const tenantSearchLoading = Vue.ref(false);
        let searchTimeout = null;
        
        // 表单数据
        const formData = Vue.reactive({
            username: '',
            tenantId: null,
            password: '',
            roleLevel: 2
        });
        
        const resetPasswordForm = Vue.reactive({
            newPassword: '',
            confirmPassword: ''
        });
        
        // 表单验证规则
        const formRules = {
            username: [
                { required: true, message: '请输入用户名', trigger: 'blur' },
                { min: 3, max: 20, message: '用户名长度在 3 到 20 个字符', trigger: 'blur' },
                {
                    validator: async (rule, value, callback) => {
                        if (!value) {
                            callback();
                            return;
                        }
                        
                        try {
                            const userInfo = JSON.parse(localStorage.getItem('user') || '{}');
                            // 超级管理员使用表单中选择的租户 ID，普通管理员使用自身租户 ID
                            const tenantId = isSuperAdmin.value ? formData.tenantId : userInfo.tenantId;
                            
                            // 未选择租户时暂不校验
                            if (!tenantId) {
                                callback();
                                return;
                            }
                            
                            // 如果是编辑模式，需要排除当前用户
                            if (dialogType.value === 'edit' && editingUser.value && editingUser.value.username === value) {
                                callback();
                                return;
                            }
                            
                            const exists = await window.api.user.checkUsername(tenantId, value);
                            if (exists) {
                                callback(new Error('该用户名已被使用'));
                            } else {
                                callback();
                            }
                        } catch (error) {
                            console.error('检查用户名失败:', error);
                            callback(new Error('检查用户名失败，请稍后重试'));
                        }
                    },
                    trigger: 'blur'
                }
            ],
            tenantId: [
                { required: true, message: '请选择租户', trigger: 'change' }
            ],
            roleLevel: [
                { required: true, message: '请选择等级', trigger: 'change' }
            ],
            password: [
                { required: true, message: '请输入密码', trigger: 'blur' },
                {
                    validator: (rule, value, callback) => {
                        if (!value) {
                            callback();
                            return;
                        }
                        
                        if (value.length < 8) {
                            callback(new Error('密码长度至少 8 位'));
                            return;
                        }
                        
                        if (!/[0-9]/.test(value)) {
                            callback(new Error('密码必须包含数字'));
                            return;
                        }
                        
                        if (!/[a-zA-Z]/.test(value)) {
                            callback(new Error('密码必须包含字母'));
                            return;
                        }
                        
                        const weakPasswords = ['123456', 'password', 'admin', '12345678', 'qwerty', '123456789', '12345', '1234567', 'letmein', '111111'];
                        if (weakPasswords.includes(value.toLowerCase())) {
                            callback(new Error('密码过于简单，请使用更复杂的密码'));
                            return;
                        }
                        
                        callback();
                    },
                    trigger: 'blur'
                }
            ]
        };
        
        const resetPasswordRules = {
            newPassword: [
                { required: true, message: '请输入新密码', trigger: 'blur' },
                {
                    validator: (rule, value, callback) => {
                        if (!value) {
                            callback();
                            return;
                        }
                        
                        if (value.length < 8) {
                            callback(new Error('密码长度至少 8 位'));
                            return;
                        }
                        
                        if (!/[0-9]/.test(value)) {
                            callback(new Error('密码必须包含数字'));
                            return;
                        }
                        
                        if (!/[a-zA-Z]/.test(value)) {
                            callback(new Error('密码必须包含字母'));
                            return;
                        }
                        
                        const weakPasswords = ['123456', 'password', 'admin', '12345678', 'qwerty', '123456789', '12345', '1234567', 'letmein', '111111'];
                        if (weakPasswords.includes(value.toLowerCase())) {
                            callback(new Error('密码过于简单，请使用更复杂的密码'));
                            return;
                        }
                        
                        callback();
                    },
                    trigger: 'blur'
                }
            ],
            confirmPassword: [
                { required: true, message: '请确认新密码', trigger: 'blur' },
                {
                    validator: (rule, value, callback) => {
                        if (value !== resetPasswordForm.newPassword) {
                            callback(new Error('两次输入的密码不一致'));
                        } else {
                            callback();
                        }
                    },
                    trigger: 'blur'
                }
            ]
        };
        
        // 远程搜索租户
        const searchTenants = (query) => {
            if (searchTimeout) clearTimeout(searchTimeout);
            
            searchTimeout = setTimeout(async () => {
                if (!query) {
                    tenantOptions.value = [];
                    return;
                }
                
                tenantSearchLoading.value = true;
                try {
                    const result = await window.api.user.searchTenants(query);
                    tenantOptions.value = result;
                } catch (error) {
                    console.error('搜索租户失败:', error);
                } finally {
                    tenantSearchLoading.value = false;
                }
            }, 300); // 300ms 防抖
        };
        
        // 加载用户列表
        const loadUsers = async () => {
            loading.value = true;
            try {// 从本地存储获取租户 ID 和角色级别
                const userInfo = JSON.parse(localStorage.getItem('user') || '{}');
                const tenantId = userInfo.tenantId !== undefined ? userInfo.tenantId : 1;
                const currentUserRoleLevel = userInfo.roleLevel !== undefined ? userInfo.roleLevel : 2;
                
                console.log('【加载用户列表】当前登录用户:', userInfo.username, 'tenantId:', tenantId, 'roleLevel:', currentUserRoleLevel);
                
                // 调用分页接口，传递当前用户角色级别用于权限过滤
                const result = await window.api.user.getList(tenantId, page.current, page.size, searchKeyword.value, currentUserRoleLevel);
                usersList.value = result.records || [];
                page.total = result.total || 0;
            } catch (error) {
                console.error('加载用户列表失败:', error);
                ElementPlus.ElMessage.error('网络错误，请稍后重试');
            } finally {
                loading.value = false;
            }
        };
        
        // 搜索处理
        const handleSearch = () => {
            page.current = 1; // 重置到第一页
            loadUsers();
        };
        
        const handleClearSearch = () => {
            searchKeyword.value = '';
            page.current = 1; // 重置到第一页
            loadUsers();
        };
        
        // 分页处理
        const handleSizeChange = (size) => {
            page.size = size;
            page.current = 1;
            loadUsers();
        };
        
        const handleCurrentChange = (current) => {
            page.current = current;
            loadUsers();
        };
        
        // 新建用户
        const handleCreateUser = () => {
            dialogType.value = 'create';
            editingUser.value = null;
            Object.assign(formData, {
                username: '',
                tenantId: null,
                password: '',
                roleLevel: 2
            });
            dialogVisible.value = true;
        };
        
        // 【保留】查看功能待后续实现，请勿删除
        /*
        const handleView = (row) => {
            console.log('【查看用户】========== 开始 ==========');
            console.log('【查看用户】1. 接收到的row:', row);
            console.log('【查看用户】2. row类型:', typeof row);
            console.log('【查看用户】3. row是否为null:', row === null);
            console.log('【查看用户】4. row是否为undefined:', row === undefined);
            
            if (!row) {
                console.error('【查看用户】错误: row为空!');
                return;
            }
            
            console.log('【查看用户】5. 设置viewUser...');
            viewUser.value = row;
            console.log('【查看用户】6. viewUser已设置:', viewUser.value);
            
            console.log('【查看用户】7. 准备显示对话框...');
            nextTick(() => {
                console.log('【查看用户】8. nextTick执行中...');
                viewDialogVisible.value = true;
                console.log('【查看用户】9. viewDialogVisible设置为:', viewDialogVisible.value);
                console.log('【查看用户】10. 当前viewUser.username:', viewUser.value?.username);
                
                // 强制检查DOM
                setTimeout(() => {
                    const dialog = document.querySelector('.el-dialog');
                    const overlay = document.querySelector('.el-overlay');
                    console.log('【查看用户】11. DOM检查 - dialog存在:', !!dialog);
                    console.log('【查看用户】12. DOM检查 - overlay存在:', !!overlay);
                    if (dialog) {
                        console.log('【查看用户】13. dialog display:', getComputedStyle(dialog).display);
                        console.log('【查看用户】14. dialog zIndex:', getComputedStyle(dialog).zIndex);
                    }
                }, 100);
                
                console.log('【查看用户】========== 结束 ==========');
            });
        };
        */
        
        // 【保留】编辑功能待后续实现，请勿删除
        /*
        const handleEdit = (user) => {
            dialogType.value = 'edit';
            editingUser.value = user;
            Object.assign(formData, {
                username: user.username,
                realName: user.realName,
                email: user.email,
                password: '', // 编辑时不显示密码
                roleLevel: user.roleLevel,
                status: user.status
            });
            dialogVisible.value = true;
        };
        */
        
        // 提交表单 - 新建用户
        const handleSubmit = async () => {
            try {
                await formRef.value.validate();
                submitting.value = true;
                
                const userInfo = JSON.parse(localStorage.getItem('user') || '{}');
                const tenantId = userInfo.tenantId !== undefined ? userInfo.tenantId : 1;
                
                if (dialogType.value === 'create') {
                    await window.api.user.create({
                        tenantId: isSuperAdmin.value ? formData.tenantId : tenantId,
                        username: formData.username,
                        password: formData.password,
                        roleLevel: formData.roleLevel
                    });
                    ElementPlus.ElMessage.success('用户创建成功');
                }
                // TODO: 编辑功能待实现
                /*
                else {
                    await window.api.user.update({
                        id: currentUser.value.id,
                        username: formData.username,
                        realName: formData.realName,
                        email: formData.email,
                        roleLevel: formData.roleLevel,
                        status: formData.status
                    });
                    ElementPlus.ElMessage.success('用户更新成功');
                }
                */
                
                dialogVisible.value = false;
                loadUsers();
            } catch (error) {
                console.error('操作失败:', error);
                ElementPlus.ElMessage.error('操作失败，请稍后重试');
            } finally {
                submitting.value = false;
            }
        };
        
        // 重置密码
        const handleResetPassword = (user) => {
            editingUser.value = user;
            Object.assign(resetPasswordForm, {
                newPassword: '',
                confirmPassword: ''
            });
            resetPasswordDialogVisible.value = true;
        };

        // 删除用户
        const handleDelete = async (user) => {
            try {
                // 构建详细的确认信息
                const roleText = user.roleLevel === 0 ? '超级管理员' : (user.roleLevel === 1 ? '普通管理员' : '操作员');
                const statusText = user.status === 1 ? '启用' : '禁用';
                const confirmMessage = `
                    <div style="text-align: left; line-height: 1.8;">
                        <p><strong>用户名：</strong>${user.username}</p>
                        <p><strong>真实姓名：</strong>${user.realName || '-'}</p>
                        <p><strong>角色：</strong>${roleText}</p>
                        <p><strong>状态：</strong>${statusText}</p>
                        <p style="color: #f56c6c; margin-top: 12px;"><strong>⚠️ 此操作不可恢复，确定要删除吗？</strong></p>
                    </div>
                `;

                await ElementPlus.ElMessageBox.confirm(
                    confirmMessage,
                    '删除用户确认',
                    {
                        confirmButtonText: '确定删除',
                        cancelButtonText: '取消',
                        type: 'warning',
                        dangerouslyUseHTMLString: true,
                        customClass: 'delete-confirm-dialog',
                    }
                );

                await window.api.user.delete(user.id);
                ElementPlus.ElMessage.success('用户删除成功');
                loadUsers();
            } catch (error) {
                if (error !== 'cancel') {
                    console.error('删除失败:', error);
                    // 根据错误类型显示不同的提示
                    if (error.message && error.message.includes('不允许删除超级管理员')) {
                        ElementPlus.ElMessage.error('不允许删除超级管理员账号');
                    } else if (error.message && error.message.includes('用户不存在')) {
                        ElementPlus.ElMessage.warning('用户不存在或已被删除');
                        loadUsers(); // 刷新列表
                    } else {
                        ElementPlus.ElMessage.error('删除失败，请稍后重试');
                    }
                }
            }
        };

        // 提交重置密码
        const handleResetPasswordSubmit = async () => {
            try {
                await resetPasswordFormRef.value.validate();
                resettingPassword.value = true;
                
                await window.api.user.resetPassword(editingUser.value.id, resetPasswordForm.newPassword);
                ElementPlus.ElMessage.success('密码重置成功');
                resetPasswordDialogVisible.value = false;
            } catch (error) {
                console.error('密码重置失败:', error);
                ElementPlus.ElMessage.error('密码重置失败，请稍后重试');
            } finally {
                resettingPassword.value = false;
            }
        };
        

        
        // 获取角色名称
        const getRoleName = (roleLevel) => {
            switch (roleLevel) {
                case 0: return '超级管理员';
                case 1: return '普通管理员';
                case 2: return '操作员';
                default: return '未知角色';
            }
        };
        
        // 检查是否允许删除用户
        const canDeleteUser = (user) => {
            // 当前登录用户信息
            const currentUser = JSON.parse(localStorage.getItem('user') || '{}');
            
            // 不允许删除超级管理员
            if (user.roleLevel === 0) {
                return false;
            }
            
            // 超级管理员可以删除任何人（除了自己）
            if (currentUser.roleLevel === 0) {
                return true;
            }
            
            // 普通管理员只能删除操作员
            if (currentUser.roleLevel === 1 && user.roleLevel === 2) {
                return true;
            }
            
            return false;
        };
        
        // 获取删除禁用的原因
        const getDeleteDisabledReason = (user) => {
            // 当前登录用户信息
            const currentUser = JSON.parse(localStorage.getItem('user') || '{}');
            
            if (user.roleLevel === 0) {
                return '不允许删除超级管理员';
            }
            
            if (currentUser.roleLevel === 1 && user.roleLevel !== 2) {
                return '普通管理员只能删除操作员';
            }
            
            if (currentUser.roleLevel === 2) {
                return '操作员没有删除权限';
            }
            
            return '';
        };
        
        // 检查是否允许重置密码
        const canResetPassword = (user) => {
            // 当前登录用户信息
            const currentUser = JSON.parse(localStorage.getItem('user') || '{}');
            
            // 不允许重置超级管理员密码
            if (user.roleLevel === 0) {
                return false;
            }
            
            // 超级管理员可以重置任何人密码
            if (currentUser.roleLevel === 0) {
                return true;
            }
            
            // 普通管理员只能重置操作员密码
            if (currentUser.roleLevel === 1 && user.roleLevel === 2) {
                return true;
            }
            
            return false;
        };
        
        // 获取重置密码禁用的原因
        const getResetPasswordDisabledReason = (user) => {
            // 当前登录用户信息
            const currentUser = JSON.parse(localStorage.getItem('user') || '{}');
            
            if (user.roleLevel === 0) {
                return '不允许重置超级管理员密码';
            }
            
            if (currentUser.roleLevel === 1 && user.roleLevel !== 2) {
                return '普通管理员只能重置操作员密码';
            }
            
            if (currentUser.roleLevel === 2) {
                return '操作员没有重置密码权限';
            }
            
            return '';
        };

        // 获取角色标签类型
        const getRoleTagType = (roleLevel) => {
            switch (roleLevel) {
                case 0: return 'danger'; // 超级管理员 - 红色
                case 1: return 'warning'; // 普通管理员 - 橙色
                case 2: return 'success'; // 操作员 - 绿色
                default: return 'info';
            }
        };
        
        // 格式化日期
        const formatDate = (dateString) => {
            if (!dateString) return '-';
            const date = new Date(dateString);
            return date.toLocaleString('zh-CN');
        };
        
        // 初始化
        onMounted(() => {
            loadUsers();
        });
        
        return {
            isSuperAdmin,
            tenantOptions,
            tenantSearchLoading,
            usersList,
            loading,
            searchKeyword,
            page,
            dialogVisible,
            dialogType,
            formData,
            formRef,
            submitting,
            // 【保留】查看功能待后续实现，请勿删除
            // viewDialogVisible,
            // viewUser,
            resetPasswordDialogVisible,
            resettingPassword,
            resetPasswordForm,
            resetPasswordFormRef,
            formRules,
            resetPasswordRules,
            
            handleSearch,
            handleClearSearch,
            // 【保留】查看/编辑功能待后续实现，请勿删除
            // handleView,
            handleCreateUser,
            // handleEdit,
            handleSubmit,
            handleResetPassword,
            handleResetPasswordSubmit,
            handleDelete,
            handleSizeChange,
            handleCurrentChange,
            getRoleName,
            getRoleTagType,
            formatDate,
            canDeleteUser,
            getDeleteDisabledReason,
            canResetPassword,
            getResetPasswordDisabledReason,
            searchTenants,
            currentUserRoleLevel
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
    console.error('Element Plus 资源未加载（CDN 失败）。');
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