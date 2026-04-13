package com.myproject.service_ai_assistant.annotation;

import com.myproject.service_ai_assistant.common.LevelCode;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 角色权限校验注解
 * 用于方法级别细粒度权限控制
 * 
 * 使用示例：
 * @RequireRole(minLevel = RoleLevelCode.ROLE_LEVEL_ADMIN)  // 管理员及以上
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RequireRole {
    /**
     * 最低角色等级要求
     * 参考 {@link LevelCode}
     * ROLE_LEVEL_SUPER_ADMIN = 0 (超级管理员)
     * ROLE_LEVEL_ADMIN = 1 (普通管理员)
     * ROLE_LEVEL_OPERATOR = 2 (操作员)
     */
    int minLevel() default 1;
}
