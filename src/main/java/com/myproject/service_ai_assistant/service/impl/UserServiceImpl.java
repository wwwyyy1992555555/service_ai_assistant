package com.myproject.service_ai_assistant.service.impl;

import cn.hutool.crypto.digest.BCrypt;
import com.myproject.service_ai_assistant.dto.LoginRequest;
import com.myproject.service_ai_assistant.dto.UserDTO;
import com.myproject.service_ai_assistant.entity.User;
import com.myproject.service_ai_assistant.exception.BusinessException;
import com.myproject.service_ai_assistant.mapper.UserMapper;
import com.myproject.service_ai_assistant.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 用户服务实现类
 */
@Slf4j
@Service
public class UserServiceImpl implements UserService {

    @Autowired
    private UserMapper userMapper;

    /**
     * 用户登录
     * TODO: 实现单点登录（SSO）或多设备登录控制
     * TODO: 使用 Redis 存储用户会话信息
     * TODO: 实现 Token 过期和刷新机制
     * TODO: 实现异地登录检测和踢人功能
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public UserDTO login(LoginRequest request) {
        log.info("【用户登录】开始登录，username={}", request.getUsername());

        // 1. 根据用户名查询用户
        User user = userMapper.selectByUsername(request.getTenantId(), request.getUsername());
        if (user == null) {
            log.warn("【用户登录】用户不存在：username={}", request.getUsername());
            throw new BusinessException(401, "用户名或密码错误");
        }
        log.info("【用户登录】用户已找到：userId={}, username={}", user.getId(), user.getUsername());

        // 2. 验证密码
        boolean passwordMatch = BCrypt.checkpw(request.getPassword(), user.getPassword());
        log.info("【用户登录】密码验证结果：{}", passwordMatch ? "匹配" : "不匹配");
        if (!passwordMatch) {
            log.warn("【用户登录】密码错误：username={}", request.getUsername());
            throw new BusinessException(401, "用户名或密码错误");
        }

        // 3. 检查用户状态
        if (user.getStatus() == 0) {
            log.warn("【用户登录】用户已被禁用：username={}", request.getUsername());
            throw new BusinessException(403, "账号已被禁用，请联系管理员");
        }

        // 4. 更新登录信息
        user.setLastLoginTime(LocalDateTime.now());
        // TODO: 获取真实 IP
        user.setLastLoginIp("127.0.0.1");
        userMapper.updateById(user);

        // 5. 转换为 DTO
        UserDTO userDTO = new UserDTO();
        BeanUtils.copyProperties(user, userDTO);
        // 不返回密码

        // 6. 生成 Token（简单实现，生产环境建议使用 JWT）
        // TODO: 使用 JWT 生成 Token，包含用户信息、过期时间等
        // TODO: 将 Token 存入 Redis，设置过期时间
        // TODO: 实现单点登录：登录前检查 Redis 中是否已有 Token，有则踢掉旧会话
        String token = generateToken(user);
        userDTO.setToken(token);

        log.info("【用户登录成功】username={}, userId={}", request.getUsername(), user.getId());
        return userDTO;
    }

    @Override
    public UserDTO getUserById(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(404, "用户不存在");
        }

        UserDTO userDTO = new UserDTO();
        BeanUtils.copyProperties(user, userDTO);
        // 不返回密码
        return userDTO;
    }

    /**
     * 生成 Token（简单实现）
     * TODO: 生产环境建议使用 JWT
     */
    private String generateToken(User user) {
        // 简单实现：UUID + 时间戳
        return UUID.randomUUID().toString().replace("-", "") + "_" + System.currentTimeMillis();
    }
}
