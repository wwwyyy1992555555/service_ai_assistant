  /**
 * 用户认证与权限工具模块
 * 统一管理用户信息获取、权限校验等公共逻辑
 */

// ==================== 用户信息缓存 ====================
let _cachedUser = null;
let _cacheTimestamp = 0;
const CACHE_DURATION = 5000; // 5秒缓存，避免频繁读取localStorage

/**
 * 获取当前登录用户信息（带缓存）
 * @returns {Object} 用户信息对象
 */
export function getCurrentUser() {
    const now = Date.now();
    
    // 如果缓存有效，直接返回
    if (_cachedUser && (now - _cacheTimestamp) < CACHE_DURATION) {
        return _cachedUser;
    }
    
    // 重新读取并缓存
    try {
        const userStr = localStorage.getItem('user');
        _cachedUser = userStr ? JSON.parse(userStr) : {};
        _cacheTimestamp = now;
    } catch (error) {
        console.error('解析用户信息失败:', error);
        _cachedUser = {};
    }
    
    return _cachedUser;
}

/**
 * 清除用户信息缓存（登出或用户信息变更时调用）
 */
export function clearUserCache() {
    _cachedUser = null;
    _cacheTimestamp = 0;
}

/**
 * 获取当前租户ID
 * @param {number} defaultTenantId - 默认租户ID（未登录时使用）
 * @returns {number} 租户ID
 */
export function getCurrentTenantId(defaultTenantId = 1) {
    const user = getCurrentUser();
    return user.tenantId !== undefined ? user.tenantId : defaultTenantId;
}

/**
 * 获取当前用户角色等级
 * @param {number} defaultRoleLevel - 默认角色等级
 * @returns {number} 角色等级
 */
export function getCurrentRoleLevel(defaultRoleLevel = 2) {
    const user = getCurrentUser();
    return user.roleLevel !== undefined ? Number(user.roleLevel) : defaultRoleLevel;
}

/**
 * 判断是否为超级管理员（tenantId === 0）
 * @returns {boolean}
 */
export function isSuperAdmin() {
    const user = getCurrentUser();
    return user.tenantId === 0;
}

/**
 * 判断是否具有管理员权限
 * 条件：tenantId=0 或 roleLevel <= 1
 * @returns {boolean}
 */
export function hasAdminPermission() {
    const user = getCurrentUser();
    return user.tenantId === 0 || (user.roleLevel !== undefined && user.roleLevel <= 1);
}

/**
 * 检查是否已登录
 * @returns {boolean}
 */
export function isAuthenticated() {
    const token = localStorage.getItem('token');
    const userStr = localStorage.getItem('user');
    return !!(token && userStr);
}

/**
 * 登出系统（清除所有本地数据）
 * @param {string} redirectUrl - 登出后跳转的URL
 */
export function logout(redirectUrl = '/login') {
    localStorage.removeItem('token');
    localStorage.removeItem('user');
    localStorage.removeItem('tenantConfig');
    clearUserCache();
    
    // 整页跳转，不使用 iframe
    window.location.replace(redirectUrl);
}

/**
 * 要求登录（未登录则自动跳转）
 * @param {string} loginUrl - 登录页面URL
 */
export function requireAuth(loginUrl = '/login') {
    if (!isAuthenticated()) {
        logout(loginUrl);
        throw new Error('未登录');
    }
}

/**
 * 要求管理员权限（无权限则提示并跳转）
 * @param {string} fallbackUrl - 无权限时跳转的URL
 */
export function requireAdminPermission(fallbackUrl = '/admin') {
    requireAuth();
    
    if (!hasAdminPermission()) {
        ElementPlus.ElMessage.error('您没有权限访问此页面');
        setTimeout(() => {
            window.top.location.href = fallbackUrl;
        }, 1500);
        throw new Error('无管理员权限');
    }
}

/**
 * 要求超级管理员权限（tenantId === 0）
 * @param {string} fallbackUrl - 无权限时跳转的URL
 */
export function requireSuperAdmin(fallbackUrl = '/admin') {
    requireAuth();
    
    if (!isSuperAdmin()) {
        ElementPlus.ElMessage.error('仅超级管理员可访问');
        setTimeout(() => {
            window.top.location.href = fallbackUrl;
        }, 1500);
        throw new Error('非超级管理员');
    }
}
