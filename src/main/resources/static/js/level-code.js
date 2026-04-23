/**
 * 前端权限等级常量（与后端 LevelCode 保持一致）
 * 所有角色等级相关的判断都使用此常量，避免硬编码数字
 */

const LEVEL_CODE = {
    // ========== 角色等级定义 ==========
    /**
     * 超级管理员/平台方/开发者（tenant_id = 0）
     * 拥有全平台管理权限
     */
    ROLE_LEVEL_PROVIDER: 0,
    
    /**
     * 租户超级管理员（租户内最高权限）
     */
    ROLE_LEVEL_SUPER_ADMIN: 0,
    
    /**
     * 租户管理员（租户内普通管理权限）
     */
    ROLE_LEVEL_ADMIN: 1,
    
    /**
     * 操作员（租户内基础操作权限）
     */
    ROLE_LEVEL_OPERATOR: 2,
    
    // ========== 租户ID特殊值 ==========
    /**
     * 平台方/开发者的租户ID
     */
    TENANT_ID_PLATFORM: 0,
    
    // ========== 权限判断辅助方法 ==========
    
    /**
     * 判断是否为平台方/超级管理员（tenantId = 0）
     * @param {number} tenantId - 租户ID
     * @returns {boolean}
     */
    isPlatformProvider(tenantId) {
        return tenantId === this.TENANT_ID_PLATFORM;
    },
    
    /**
     * 判断是否具有管理员权限（roleLevel <= 1）
     * 包括：超级管理员(0) 和 管理员(1)
     * @param {number} roleLevel - 角色等级
     * @returns {boolean}
     */
    hasAdminPermission(roleLevel) {
        return roleLevel !== undefined && roleLevel <= this.ROLE_LEVEL_ADMIN;
    },
    
    /**
     * 判断是否可以管理指定等级的用户
     * 规则：只能管理等级数值大于自己的用户（等级数值越大，权限越低）
     * @param {number} operatorLevel - 操作者等级
     * @param {number} targetLevel - 目标用户等级
     * @returns {boolean}
     */
    canManageUser(operatorLevel, targetLevel) {
        // 平台方可以管理所有用户
        if (operatorLevel === this.ROLE_LEVEL_PROVIDER) {
            return true;
        }
        // 只能管理等级数值更大的用户（权限更低）
        return targetLevel > operatorLevel;
    },
    
    /**
     * 获取当前用户可以创建的最大等级
     * @param {number} currentLevel - 当前用户等级
     * @returns {number} 可创建的最小等级数值（最大权限）
     */
    getMaxCreateableLevel(currentLevel) {
        // 平台方和管理员可以创建操作员
        if (currentLevel <= this.ROLE_LEVEL_ADMIN) {
            return this.ROLE_LEVEL_OPERATOR;
        }
        // 操作员不能创建其他用户
        return null;
    },
    
    /**
     * 获取角色等级的中文名称
     * @param {number} roleLevel - 角色等级
     * @returns {string}
     */
    getRoleLevelName(roleLevel) {
        const names = {
            [this.ROLE_LEVEL_PROVIDER]: '平台方',
            [this.ROLE_LEVEL_SUPER_ADMIN]: '超级管理员',
            [this.ROLE_LEVEL_ADMIN]: '管理员',
            [this.ROLE_LEVEL_OPERATOR]: '操作员'
        };
        return names[roleLevel] || '未知';
    },
    
    /**
     * 获取角色等级的描述
     * @param {number} roleLevel - 角色等级
     * @returns {string}
     */
    getRoleLevelDescription(roleLevel) {
        const descriptions = {
            [this.ROLE_LEVEL_PROVIDER]: '平台方/开发者（全平台管理权限）',
            [this.ROLE_LEVEL_SUPER_ADMIN]: '超级管理员（租户内最高权限）',
            [this.ROLE_LEVEL_ADMIN]: '管理员（租户内普通管理权限）',
            [this.ROLE_LEVEL_OPERATOR]: '操作员（租户内基础操作权限）'
        };
        return descriptions[roleLevel] || '未知角色';
    }
};

// 导出供全局使用
if (typeof window !== 'undefined') {
    window.LEVEL_CODE = LEVEL_CODE;
}
