package com.myproject.service_ai_assistant.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.myproject.service_ai_assistant.dto.LoginRequest;
import com.myproject.service_ai_assistant.dto.UserDTO;
import com.myproject.service_ai_assistant.dto.UserCreateRequest;

/**
 * 用户服务接口
 */
public interface UserService {

    /**
     * 用户登录
     * @param request 登录请求
     * @return 用户信息（包含 token）
     */
    UserDTO login(LoginRequest request);

    /**
     * 根据 ID 获取用户信息
     * @param userId 用户 ID
     * @return 用户信息
     */
    UserDTO getUserById(Long userId);

    /**
     * 修改密码
     * @param userId 用户 ID
     * @param oldPassword 旧密码
     * @param newPassword 新密码
     */
    void changePassword(Long userId, String oldPassword, String newPassword);
    
    /**
     * 创建用户
     * @param request 创建用户请求
     * @return 创建的用户信息
     */
    UserDTO createUser(UserCreateRequest request);
    
    /**
     * 重置用户密码
     * @param userId 用户 ID
     * @param newPassword 新密码
     */
    void resetPassword(Long userId, String newPassword);
    
    /**
     * 禁用/启用用户
     * @param userId 用户 ID
     * @param status 状态（1-启用，0-禁用）
     */
    void updateUserStatus(Long userId, Integer status);
    
    /**
     * 根据租户 ID 获取用户列表
     * @param tenantId 租户 ID
     * @param current 当前页码
     * @param size 每页大小
     * @param keyword 搜索关键词
     * @param currentUserRoleLevel 当前登录用户的角色级别（用于权限过滤）
     * @return 分页用户列表
     */
    Page<UserDTO> getUsersByTenantId(Long tenantId, Integer current, Integer size, String keyword, Integer currentUserRoleLevel);
    
    /**
     * 删除用户
     * @param userId 用户 ID
     */
    void deleteUser(Long userId);
    
    /**
     * 检查用户名在租户下是否已存在
     * @param tenantId 租户 ID
     * @param username 用户名
     * @return true-已存在，false-不存在
     */
    boolean isUsernameExists(Long tenantId, String username);
}