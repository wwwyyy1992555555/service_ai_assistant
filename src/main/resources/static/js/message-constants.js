/**
 * 前端消息常量统一管理
 * 所有提示信息、错误信息都从这里引用，避免硬编码
 */

const MESSAGE = {
    // ========== 通用消息 ==========
    SUCCESS: {
        OPERATION: '操作成功',
        SAVE: '保存成功',
        DELETE: '删除成功',
        UPDATE: '更新成功',
        CREATE: '创建成功',
        VERIFY_CODE_SENT: '验证码已发送至您的邮箱',
        REGISTER_SUCCESS: '注册成功！请使用 tenant_code 配置 chat 页面'
    },
    
    ERROR: {
        NETWORK: '网络连接失败，请检查网络',
        SERVER_ERROR: '服务器内部错误，请稍后重试',
        REQUEST_FAILED: '请求失败',
        NON_JSON_RESPONSE: '服务器返回错误（{status}），请稍后重试',
        
        // 认证相关
        LOGIN_REQUIRED: '请先登录',
        LOGIN_EXPIRED: '登录已过期，请重新登录',
        AUTH_FAILED: '用户名或密码错误',
        ACCOUNT_LOCKED: '账号已被锁定，请 {minutes} 分钟后再试',
        ACCOUNT_DISABLED: '账号已被禁用，请联系管理员',
        MULTI_LOGIN: '您的账号已在其他设备登录，请重新登录',
        
        // 租户相关
        TENANT_NOT_FOUND: '租户不存在',
        TENANT_DISABLED: '租户未启用',
        TENANT_EXPIRED: '租户已过期',
        TENANT_CODE_EXISTS: '租户编码已存在',
        TENANT_DELETE_FAILED: '只能删除已禁用的租户，请先禁用该租户',
        TENANT_UPDATE_FAILED: '更新租户信息失败',
        TENANT_CREATE_FAILED: '创建租户失败',
        TENANT_NAME_REGISTERED: '该企业已注册，请联系管理员或找回密码',
        EMAIL_REGISTERED: '该邮箱已注册，请更换邮箱或找回密码',
        VERIFY_CODE_EXPIRED: '验证码已过期或不存在',
        VERIFY_CODE_ERROR: '验证码错误',
        
        // 用户相关
        USER_NOT_FOUND: '用户不存在',
        USERNAME_EXISTS: '用户名已存在',
        PHONE_EXISTS: '手机号已存在',
        OLD_PASSWORD_WRONG: '原密码错误',
        PASSWORD_CHANGE_FAILED: '修改密码失败',
        PASSWORD_RESET_FAILED: '重置密码失败',
        USER_CREATE_FAILED: '创建用户失败',
        USER_UPDATE_FAILED: '更新用户状态失败',
        USER_DELETE_FAILED: '删除用户失败',
        DELETE_SUPER_ADMIN_FORBIDDEN: '不允许删除超级管理员账号',
        PERMISSION_DENIED: '权限不足，无法执行此操作',
        PERMISSION_DENIED_USER_MANAGEMENT: '您的角色等级不足以访问用户管理模块，请联系超级管理员',
        PERMISSION_DENIED_DELETE_USER: '无权删除等级大于或等于您的用户（用户名：{username}）',
        USER_ID_NOT_EXISTS: '用户ID为{id}不存在',
        CROSS_TENANT_OPERATION_FORBIDDEN: '无权删除其他租户的用户',
        FEEDBACK_IDS_REQUIRED: '请选择要删除的反馈',
        USER_IDS_REQUIRED: '请选择要删除的用户',
        
        // 参数校验
        USERNAME_EMPTY: '用户名不能为空',
        USERNAME_LENGTH_INVALID: '用户名长度必须为 3-20 位',
        USERNAME_FORMAT_INVALID: '用户名只能包含字母、数字和下划线',
        USERNAME_CHECK_FAILED: '检查用户名失败，请稍后重试',
        USERNAME_ALREADY_USED: '该用户名已被使用',
        
        // 密码相关
        PASSWORD_LENGTH_MIN: '密码长度至少 8 位',
        PASSWORD_MUST_CONTAIN_NUMBER: '密码必须包含数字',
        PASSWORD_MUST_CONTAIN_LETTER: '密码必须包含字母',
        PASSWORD_TOO_SIMPLE: '密码过于简单，请使用更复杂的密码',
        PASSWORD_REQUIRED: '请输入密码',
        PASSWORD_CONFIRM_MISMATCH: '两次输入的密码不一致',
        
        // 表单验证
        FIELD_REQUIRED: '请输入{field}',
        FIELD_LENGTH_RANGE: '{field}长度在 {min} 到 {max} 个字符',
        TENANT_REQUIRED: '请选择租户',
        ROLE_LEVEL_REQUIRED: '请选择等级',
        
        // 知识库相关
        KNOWLEDGE_NOT_FOUND: '知识条目不存在',
        CATEGORY_NOT_FOUND: '分类不存在',
        LOAD_CATEGORIES_FAILED: '加载分类失败',
        DELETE_CATEGORY_FAILED: '删除失败',
        
        // 咨询反馈相关
        FEEDBACK_NOT_FOUND: '反馈记录不存在',
        CONSULTATION_NOT_FOUND: '咨询记录不存在',
        FEEDBACK_FORMAT_ERROR: '反馈原因格式错误',
        LOAD_FEEDBACK_FAILED: '加载反馈失败',
        SUBMIT_FEEDBACK_FAILED: '提交反馈失败',
        
        // 数据加载
        LOAD_DATA_FAILED: '加载数据失败',
        LOAD_RECORDS_FAILED: '加载记录失败',
        LOAD_TENANTS_FAILED: '加载租户列表失败',
        LOAD_INDUSTRY_TYPES_FAILED: '加载行业类型失败',
        SEARCH_TENANTS_FAILED: '搜索租户失败',
        LOAD_HOT_QUESTIONS_FAILED: '加载热门问题失败',
        LOAD_SYSTEM_CONFIG_FAILED: '加载系统配置失败',
        
        // AI 服务
        AI_SERVICE_ERROR: 'AI 服务调用失败',
        AI_SERVICE_TIMEOUT: 'AI 服务响应超时',
        SEND_MESSAGE_FAILED: '发送消息失败',
        PARSE_INPUT_FAILED: '解析用户输入失败',
        CALL_BACKEND_PARSE_FAILED: '调用后端解析失败',
        SUBMIT_SATISFACTION_FAILED: '提交满意度失败',
        
        // 文件相关
        FILE_UPLOAD_ERROR: '文件上传失败',
        FILE_TYPE_NOT_SUPPORTED: '不支持的文件类型',
        FILE_SIZE_EXCEEDED: '文件大小超出限制',
        
        // Redis 相关
        REDIS_OPERATION_FAILED: '验证码存储失败，请检查 Redis 服务',
        EMAIL_SEND_FAILED: '验证码发送失败，请稍后重试',
        
        // 其他
        LOAD_USER_INFO_FAILED: '解析用户信息失败',
        REMOVE_UNNECESSARY_CSS: '已移除不必要的 CSS',
        DETECT_CATEGORY_CHANGE: '检测到分类数据变更，重新加载...',
        USE_LOGIN_TENANT_CONFIG: '使用登录时返回的租户配置',
        USE_SEPARATE_TENANT_CONFIG: '使用单独接口加载的租户配置',
        LOAD_TENANT_CONFIG_FAILED: '加载租户配置失败'
    },
    
    // ========== 警告消息 ==========
    WARNING: {
        CONFIRM_DELETE: '确认删除吗？',
        CONFIRM_BATCH_DELETE: '确认批量删除选中的 {count} 条记录吗？',
        UNSUPPORTED_OPERATION: '不支持的操作'
    },
    
    // ========== 提示信息 ==========
    INFO: {
        VUE_LOADED: 'Vue 加载成功，版本：{version}',
        CHAT_JS_LOADED: 'chat.js 加载完成',
        API_BASE_URL: 'API_BASE_URL：{url}',
        CURRENT_TENANT_ID: 'CURRENT_TENANT_ID：{id}',
        PERMISSION_DEBUG: '【权限调试】hasAdminPermission：{result}，tenantId：{tenantId}，roleLevel：{roleLevel}',
        MENU_DEBUG: '【菜单调试】defaultMenu：{menu}，isSuperAdmin：{isAdmin}',
        USER_LEVEL_DEBUG: '【用户等级调试】currentUser.roleLevel：{level}，转换为数字：{numeric}',
        LOAD_USER_LIST_DEBUG: '【加载用户列表】当前登录用户：{username}，tenantId：{tenantId}，roleLevel：{roleLevel}'
    }
};

/**
 * 格式化消息（替换占位符）
 * @param {string} template - 消息模板
 * @param {object} params - 参数对象
 * @returns {string} 格式化后的消息
 */
function formatMessage(template, params = {}) {
    let result = template;
    for (const [key, value] of Object.entries(params)) {
        result = result.replace(new RegExp(`\\{${key}\\}`, 'g'), value);
    }
    return result;
}

// 导出供全局使用
if (typeof window !== 'undefined') {
    window.MESSAGE = MESSAGE;
    window.formatMessage = formatMessage;
}
