/**
 * 用户管理页面逻辑
 */

// ==================== 权限检查 ====================
const userStr = localStorage.getItem('user');
if (!userStr) {
    if (typeof ElementPlus !== 'undefined' && ElementPlus.ElMessage) {
        ElementPlus.ElMessage.error(MESSAGE.ERROR.LOGIN_REQUIRED);
    }
    setTimeout(() => {
        window.top.location.replace('/login');
    }, 300);
    // 防止后续代码继续执行导致报错
    throw new Error('User not logged in');
}

const currentUser = JSON.parse(userStr);

// ==================== 公共密码验证器 ====================
/**
 * 密码强度验证器（复用）
 * @param {string} value - 密码值
 * @param {Function} callback - Element Plus 验证回调
 */
function validatePasswordStrength(value, callback) {
    if (!value) {
        callback();
        return;
    }
    
    if (value.length < 8) {
        callback(new Error(MESSAGE.ERROR.PASSWORD_LENGTH_MIN));
        return;
    }
    
    if (!/[0-9]/.test(value)) {
        callback(new Error(MESSAGE.ERROR.PASSWORD_MUST_CONTAIN_NUMBER));
        return;
    }
    
    if (!/[a-zA-Z]/.test(value)) {
        callback(new Error(MESSAGE.ERROR.PASSWORD_MUST_CONTAIN_LETTER));
        return;
    }
    
    const weakPasswords = ['123456', 'password', 'admin', '12345678', 'qwerty', '123456789', '12345', '1234567', 'letmein', '111111'];
    if (weakPasswords.includes(value.toLowerCase())) {
        callback(new Error(MESSAGE.ERROR.PASSWORD_TOO_SIMPLE));
        return;
    }
    
    callback();
}

// ==================== 用户信息辅助函数 ====================
/**
 * 获取当前租户ID（带默认值）
 */
function _getCurrentTenantId(defaultId = 1) {
    try {
        const userInfo = JSON.parse(localStorage.getItem('user') || '{}');
        return userInfo.tenantId !== undefined ? userInfo.tenantId : defaultId;
    } catch (e) {
        return defaultId;
    }
}

/**
 * 获取当前用户角色等级
 */
function _getCurrentRoleLevel(defaultLevel = 2) {
    try {
        const userInfo = JSON.parse(localStorage.getItem('user') || '{}');
        return userInfo.roleLevel !== undefined ? Number(userInfo.roleLevel) : defaultLevel;
    } catch (e) {
        return defaultLevel;
    }
}

const { createApp, onMounted, onUnmounted, nextTick } = Vue;

const app = createApp({
    setup() {
        // 是否为超级管理员
        const isSuperAdmin = Vue.ref(userStr && LEVEL_CODE.isPlatformProvider(currentUser.tenantId));
        
        // 当前登录用户的角色等级（用于控制可创建的用户等级）
        const roleLevelValue = currentUser.roleLevel !== undefined ? Number(currentUser.roleLevel) : 2;
        console.log(formatMessage(MESSAGE.INFO.USER_LEVEL_DEBUG, {
            level: currentUser.roleLevel,
            numeric: roleLevelValue
        }));
        const currentUserRoleLevel = Vue.ref(roleLevelValue);
        
        const usersList = Vue.ref([]);
        const loading = Vue.ref(false);
        const searchKeyword = Vue.ref('');
        const selectedRows = Vue.ref([]); // 选中的行
        
        // 表格高度（显式设置，避免 iframe/flex 布局导致表格被压扁）
        const tableHeight = Vue.ref(520);
        const computeTableHeight = () => {
            const reserved = 260;
            tableHeight.value = Math.max(320, window.innerHeight - reserved);
        };
        
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
                { required: true, message: formatMessage(MESSAGE.ERROR.FIELD_REQUIRED, {field: '用户名'}), trigger: 'blur' },
                { min: 3, max: 20, message: formatMessage(MESSAGE.ERROR.FIELD_LENGTH_RANGE, {field: '用户名', min: 3, max: 20}), trigger: 'blur' },
                {
                    validator: async (rule, value, callback) => {
                        if (!value) {
                            callback();
                            return;
                        }
                        
                        try {
                            const userInfo = JSON.parse(localStorage.getItem('user') || '{}');
                            // 超级管理员使用表单中选择的租户 ID，普通管理员使用自身租户 ID
                            const tenantId = isSuperAdmin.value ? formData.tenantId : _getCurrentTenantId();
                            
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
                                callback(new Error(MESSAGE.ERROR.USERNAME_ALREADY_USED));
                            } else {
                                callback();
                            }
                        } catch (error) {
                            console.error(MESSAGE.ERROR.USERNAME_CHECK_FAILED, error);
                            callback(new Error(MESSAGE.ERROR.USERNAME_CHECK_FAILED));
                        }
                    },
                    trigger: 'blur'
                }
            ],
            tenantId: [
                { required: true, message: MESSAGE.ERROR.TENANT_REQUIRED, trigger: 'change' }
            ],
            roleLevel: [
                { required: true, message: MESSAGE.ERROR.ROLE_LEVEL_REQUIRED, trigger: 'change' }
            ],
            password: [
                { required: true, message: MESSAGE.ERROR.PASSWORD_REQUIRED, trigger: 'blur' },
                {
                    validator: (rule, value, callback) => validatePasswordStrength(value, callback),
                    trigger: 'blur'
                }
            ]
        };
        
        const resetPasswordRules = {
            newPassword: [
                { required: true, message: formatMessage(MESSAGE.ERROR.FIELD_REQUIRED, {field: '新密码'}), trigger: 'blur' },
                {
                    validator: (rule, value, callback) => validatePasswordStrength(value, callback),
                    trigger: 'blur'
                }
            ],
            confirmPassword: [
                { required: true, message: formatMessage(MESSAGE.ERROR.FIELD_REQUIRED, {field: '确认新密码'}), trigger: 'blur' },
                {
                    validator: (rule, value, callback) => {
                        if (value !== resetPasswordForm.newPassword) {
                            callback(new Error(MESSAGE.ERROR.PASSWORD_CONFIRM_MISMATCH));
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
                    console.error(MESSAGE.ERROR.SEARCH_TENANTS_FAILED, error);
                } finally {
                    tenantSearchLoading.value = false;
                }
            }, 300); // 300ms 防抖
        };
        
        // 处理选择变化
        const handleSelectionChange = (selection) => {
            selectedRows.value = selection;
        };
        
        // 批量删除用户
        const handleBatchDelete = async () => {
            if (selectedRows.value.length === 0) {
                ElementPlus.ElMessage.warning(MESSAGE.ERROR.USER_IDS_REQUIRED.replace('反馈', '用户'));
                return;
            }
            
            // 前端预检：过滤出无权删除的用户
            const invalidUsers = selectedRows.value.filter(user => !canDeleteUser(user));
            if (invalidUsers.length > 0) {
                const usernames = invalidUsers.map(u => u.username).join('、');
                ElementPlus.ElMessage.error(`无权删除以下用户：${usernames}`);
                return;
            }
            
            try {
                const confirmMessage = `
                    <div style="text-align: left; line-height: 1.8;">
                        <p><strong>即将删除 ${selectedRows.value.length} 个用户</strong></p>
                        <p style="color: #f56c6c; margin-top: 12px; font-size: 14px;">
                            <strong>⚠️ 此操作不可恢复，确定要删除吗？</strong>
                        </p>
                    </div>
                `;
                
                await ElementPlus.ElMessageBox.confirm(
                    confirmMessage,
                    '批量删除确认',
                    {
                        confirmButtonText: '确定删除',
                        cancelButtonText: '取消',
                        type: 'warning',
                        dangerouslyUseHTMLString: true,
                    }
                );
                
                const userIds = selectedRows.value.map(row => row.id);
                // 超级管理员使用被删除用户所在租户的ID，普通管理员使用自身租户ID
                const tenantId = isSuperAdmin.value ? selectedRows.value[0].tenantId : _getCurrentTenantId();
                
                await window.api.user.batchDelete(tenantId, userIds);
                ElementPlus.ElMessage.success(MESSAGE.SUCCESS.DELETE);
                selectedRows.value = [];
                page.current = 1;
                loadUsers();
            } catch (error) {
                if (error !== 'cancel') {
                    ElementPlus.ElMessage.error(error?.message || MESSAGE.ERROR.USER_DELETE_FAILED);
                }
            }
        };
        
        // 加载用户列表
        const loadUsers = async () => {
            loading.value = true;
            try {
                const tenantId = _getCurrentTenantId();
                const currentUserRoleLevel = _getCurrentRoleLevel();
                
                console.log(formatMessage(MESSAGE.INFO.LOAD_USER_LIST_DEBUG, {
                    username: currentUser.username,
                    tenantId: tenantId,
                    roleLevel: currentUserRoleLevel
                }));
                
                // 调用分页接口，传递当前用户角色级别用于权限过滤
                const result = await window.api.user.getList(tenantId, page.current, page.size, searchKeyword.value, currentUserRoleLevel);
                usersList.value = result.records || [];
                page.total = result.total || 0;
            } catch (error) {
                console.error(MESSAGE.ERROR.LOAD_DATA_FAILED, error);
                ElementPlus.ElMessage.error(MESSAGE.ERROR.NETWORK);
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
                
                const tenantId = _getCurrentTenantId();
                
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
                const roleText = LEVEL_CODE.getRoleLevelName(user.roleLevel);
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

                // 获取被删除用户所在的租户ID（超级管理员跨租户操作时使用）
                const tenantId = isSuperAdmin.value ? user.tenantId : _getCurrentTenantId();
                await window.api.user.delete(tenantId, user.id);
                ElementPlus.ElMessage.success(MESSAGE.SUCCESS.DELETE);
                loadUsers();
            } catch (error) {
                if (error !== 'cancel') {
                    console.error(MESSAGE.ERROR.USER_DELETE_FAILED, error);
                    // 根据错误类型显示不同的提示
                    if (error.message && error.message.includes(LEVEL_CODE.getRoleLevelName(LEVEL_CODE.ROLE_LEVEL_SUPER_ADMIN))) {
                        ElementPlus.ElMessage.error(MESSAGE.ERROR.DELETE_SUPER_ADMIN_FORBIDDEN);
                    } else if (error.message && error.message.includes(MESSAGE.ERROR.USER_NOT_FOUND)) {
                        ElementPlus.ElMessage.warning(MESSAGE.ERROR.USER_NOT_FOUND);
                        loadUsers(); // 刷新列表
                    } else {
                        ElementPlus.ElMessage.error(MESSAGE.ERROR.USER_DELETE_FAILED);
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
                ElementPlus.ElMessage.success(MESSAGE.SUCCESS.UPDATE);
                resetPasswordDialogVisible.value = false;
            } catch (error) {
                console.error(MESSAGE.ERROR.PASSWORD_RESET_FAILED, error);
                ElementPlus.ElMessage.error(MESSAGE.ERROR.PASSWORD_RESET_FAILED);
            } finally {
                resettingPassword.value = false;
            }
        };
        

        
        // 获取角色名称
        const getRoleName = (roleLevel) => {
            return LEVEL_CODE.getRoleLevelName(roleLevel);
        };
        
        // 检查是否允许删除用户
        const canDeleteUser = (user) => {
            const currentUserRoleLevel = _getCurrentRoleLevel();
            
            // 不允许删除超级管理员
            if (user.roleLevel === LEVEL_CODE.ROLE_LEVEL_SUPER_ADMIN) {
                return false;
            }
            
            // 超级管理员可以删除任何人（除了自己）
            if (currentUserRoleLevel === LEVEL_CODE.ROLE_LEVEL_PROVIDER) {
                return true;
            }
            
            // 普通管理员只能删除操作员
            if (currentUserRoleLevel === LEVEL_CODE.ROLE_LEVEL_ADMIN && user.roleLevel === LEVEL_CODE.ROLE_LEVEL_OPERATOR) {
                return true;
            }
            
            return false;
        };
        
        // 获取删除禁用的原因
        const getDeleteDisabledReason = (user) => {
            const currentUserRoleLevel = _getCurrentRoleLevel();
            
            if (user.roleLevel === LEVEL_CODE.ROLE_LEVEL_SUPER_ADMIN) {
                return MESSAGE.ERROR.DELETE_SUPER_ADMIN_FORBIDDEN;
            }
            
            if (currentUserRoleLevel === LEVEL_CODE.ROLE_LEVEL_ADMIN && user.roleLevel !== LEVEL_CODE.ROLE_LEVEL_OPERATOR) {
                return formatMessage(MESSAGE.ERROR.PERMISSION_DENIED_DELETE_USER, {username: user.username});
            }
            
            if (currentUserRoleLevel === LEVEL_CODE.ROLE_LEVEL_OPERATOR) {
                return MESSAGE.ERROR.PERMISSION_DENIED;
            }
            
            return '';
        };
        
        // 检查是否允许重置密码
        const canResetPassword = (user) => {
            const currentUserRoleLevel = _getCurrentRoleLevel();
            
            // 不允许重置超级管理员密码
            if (user.roleLevel === LEVEL_CODE.ROLE_LEVEL_SUPER_ADMIN) {
                return false;
            }
            
            // 超级管理员可以重置任何人密码
            if (currentUserRoleLevel === LEVEL_CODE.ROLE_LEVEL_PROVIDER) {
                return true;
            }
            
            // 普通管理员只能重置操作员密码
            if (currentUserRoleLevel === LEVEL_CODE.ROLE_LEVEL_ADMIN && user.roleLevel === LEVEL_CODE.ROLE_LEVEL_OPERATOR) {
                return true;
            }
            
            return false;
        };
        
        // 获取重置密码禁用的原因
        const getResetPasswordDisabledReason = (user) => {
            const currentUserRoleLevel = _getCurrentRoleLevel();
            
            if (user.roleLevel === LEVEL_CODE.ROLE_LEVEL_SUPER_ADMIN) {
                return '不允许重置超级管理员密码';
            }
            
            if (currentUserRoleLevel === LEVEL_CODE.ROLE_LEVEL_ADMIN && user.roleLevel !== LEVEL_CODE.ROLE_LEVEL_OPERATOR) {
                return '普通管理员只能重置操作员密码';
            }
            
            if (currentUserRoleLevel === LEVEL_CODE.ROLE_LEVEL_OPERATOR) {
                return MESSAGE.ERROR.PERMISSION_DENIED;
            }
            
            return '';
        };

        // 获取角色标签类型
        const getRoleTagType = (roleLevel) => {
            switch (roleLevel) {
                case LEVEL_CODE.ROLE_LEVEL_SUPER_ADMIN: return 'danger'; // 超级管理员 - 红色
                case LEVEL_CODE.ROLE_LEVEL_ADMIN: return 'warning'; // 普通管理员 - 橙色
                case LEVEL_CODE.ROLE_LEVEL_OPERATOR: return 'success'; // 操作员 - 绿色
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
            computeTableHeight();
            window.addEventListener('resize', computeTableHeight);
            loadUsers();
        });
        
        onUnmounted(() => {
            window.removeEventListener('resize', computeTableHeight);
        });
        
        return {
            isSuperAdmin,
            tenantOptions,
            tenantSearchLoading,
            usersList,
            loading,
            searchKeyword,
            selectedRows,
            tableHeight,
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
            handleSelectionChange,
            handleBatchDelete,
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
            currentUserRoleLevel,
            tableHeight
        };
    }
});

// 统一初始化 Element Plus（异步）
async function setupApp() {
    if (typeof ElementPlus === 'undefined') {
        console.error('Element Plus 资源未加载（CDN 失败）。');
        return;
    }
    
    if (typeof initElementPlus === 'function') {
        await initElementPlus(app);
    } else {
        app.use(ElementPlus, {
            locale: typeof ElementPlusLocaleZhCn !== 'undefined' ? ElementPlusLocaleZhCn : undefined
        });
        // 降级：直接注册图标
        if (typeof ElementPlusIconsVue !== 'undefined') {
            for (const [key, component] of Object.entries(ElementPlusIconsVue)) {
                app.component(key, component);
            }
        }
    }
    
    app.mount('#app');
}

setupApp().catch(err => {
    console.error('应用初始化失败:', err);
    app.mount('#app');
});