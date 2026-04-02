package com.myproject.service_ai_assistant.service;

import com.myproject.service_ai_assistant.dto.LoginRequest;
import com.myproject.service_ai_assistant.dto.UserDTO;

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
}
