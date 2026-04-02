package com.myproject.service_ai_assistant.service.impl;

import cn.hutool.crypto.digest.BCrypt;
import com.myproject.service_ai_assistant.common.PasswordUtil;
import com.myproject.service_ai_assistant.dto.LoginRequest;
import com.myproject.service_ai_assistant.dto.UserDTO;
import com.myproject.service_ai_assistant.entity.TenantConfig;
import com.myproject.service_ai_assistant.entity.User;
import com.myproject.service_ai_assistant.exception.BusinessException;
import com.myproject.service_ai_assistant.mapper.UserMapper;
import com.myproject.service_ai_assistant.service.TenantConfigService;
import com.myproject.service_ai_assistant.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * 用户服务实现类
 */
@Slf4j
@Service
public class UserServiceImpl implements UserService {

    @Autowired
    private UserMapper userMapper;
    
    @Autowired
    private StringRedisTemplate redisTemplate;
    
    @Autowired
    private TenantConfigService tenantConfigService;
    
    /**
     * Token 有效期：7 天（秒）
     */
    private static final long TOKEN_EXPIRE_SECONDS = 7 * 24 * 60 * 60;

    /**
     * 登录失败锁定时间：30 分钟（秒）
     */
    private static final long LOCK_ACCOUNT_SECONDS = 30 * 60;

    /**
     * 最大登录失败次数
     */
    private static final int MAX_LOGIN_FAIL_COUNT = 5;

    /**
     * Redis Key 前缀：用户当前有效的 Token
     */
    private static final String USER_TOKEN_KEY_PREFIX = "user:token:";

    /**
     * 用户登录（支持租户用户和超级管理员）
     * 注意：tenant_id=0 且 role=super_admin 表示超级管理员
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public UserDTO login(LoginRequest request) {
        log.info("【用户登录】开始登录，username={}, loginType={}", request.getUsername(), request.getLoginType());
        
        // 1. 检查账号是否已被锁定
        String failCountKey = "login:fail:" + request.getUsername();
        String lockKey = "login:lock:" + request.getUsername();
        
        Boolean isLocked = redisTemplate.hasKey(lockKey);
        if (Boolean.TRUE.equals(isLocked)) {
            Long remainSeconds = redisTemplate.getExpire(lockKey, TimeUnit.SECONDS);
            log.warn("【用户登录】账号已被锁定，请稍后再试：username={}, 剩余时间={}秒", request.getUsername(), remainSeconds);
            throw new BusinessException(403, String.format("账号已被锁定，请 %d 分钟后再试", remainSeconds / 60));
        }
        
        // 2. 查询用户（超级管理员登录时 tenantId 传 null）
        Long queryTenantId = "super".equals(request.getLoginType()) ? null : request.getTenantId();
        User user = userMapper.selectByUsername(queryTenantId, request.getUsername());
        
        if (user == null) {
            log.warn("【用户登录】用户不存在：username={}", request.getUsername());
            // 记录失败次数
            incrementLoginFailCount(failCountKey, lockKey);
            throw new BusinessException(401, "用户名或密码错误");
        }
        log.info("【用户登录】用户已找到：userId={}, username={}, tenantId={}, role={}", 
                user.getId(), user.getUsername(), user.getTenantId(), user.getRole());
        
        // 3. 验证密码
        boolean passwordMatch = BCrypt.checkpw(request.getPassword(), user.getPassword());
        log.info("【用户登录】密码验证结果：{}", passwordMatch ? "匹配" : "不匹配");
        if (!passwordMatch) {
            log.warn("【用户登录】密码错误：username={}", request.getUsername());
            // 记录失败次数
            incrementLoginFailCount(failCountKey, lockKey);
            throw new BusinessException(401, "用户名或密码错误");
        }
        
        // 3. 检查用户状态
        if (user.getStatus() == 0) {
            log.warn("【用户登录】用户已被禁用：username={}", request.getUsername());
            throw new BusinessException(403, "账号已被禁用，请联系管理员");
        }
        
        // 4. 更新登录信息
        user.setLastLoginTime(LocalDateTime.now());
        user.setLastLoginIp("127.0.0.1");
        userMapper.updateById(user);
        
        // 5. 转换为 DTO
        UserDTO userDTO = new UserDTO();
        BeanUtils.copyProperties(user, userDTO);
        // 不返回密码
        
        // 6. 填充租户信息（优化：一次性返回完整租户配置）
        if ("super".equals(request.getLoginType()) || ("super_admin".equals(user.getRole()) && user.getTenantId() != null && user.getTenantId() == 0)) {
            // 超级管理员
            log.info("【用户登录】超级管理员登录成功：username={}", request.getUsername());
            userDTO.setTenantId(0L);
            userDTO.setTenantName("平台管理端");
            // 超级管理员不需要租户配置信息
        } else {
            // 租户用户 - 查询租户配置信息
            log.info("【用户登录】租户用户登录成功：username={}, tenantId={}", request.getUsername(), user.getTenantId());
            try {
                TenantConfig config = tenantConfigService.getByTenantId(user.getTenantId());
                if (config != null) {
                    userDTO.setTenantLogoUrl(config.getLogoUrl());
                    userDTO.setTenantThemeColor(config.getThemeColor());
                    userDTO.setTenantWelcomeMessage(config.getWelcomeMessage());
                    log.info("【登录优化】已加载租户配置：tenantId={}, companyName={}", user.getTenantId(), config.getCompanyName());
                }
            } catch (Exception e) {
                log.warn("【登录优化】获取租户配置失败（使用默认值）：tenantId={}", user.getTenantId(), e);
                // 即使获取失败也不影响登录，使用默认值即可
            }
        }
        
        // 7. 生成 Token
        String token = generateToken(user);
        userDTO.setToken(token);
        
        // 8. 单设备登录控制 - 使旧设备的 Token 失效
        String userTokenKey = USER_TOKEN_KEY_PREFIX + user.getId();
        String oldToken = redisTemplate.opsForValue().get(userTokenKey);
        if (oldToken != null) {
            // 删除旧 Token
            String oldRedisKey = "token:" + oldToken;
            redisTemplate.delete(oldRedisKey);
            log.info("【单设备登录】旧设备 Token 已失效：userId={}, oldToken={}", user.getId(), oldToken);
        }
        
        // 保存新 Token 到用户 - Token 映射
        redisTemplate.opsForValue().set(userTokenKey, token, TOKEN_EXPIRE_SECONDS, TimeUnit.SECONDS);
        log.info("【单设备登录】保存新 Token：userId={}, token={}", user.getId(), token);
        
        // 9. 将 Token 存入 Redis（实现登录状态控制）
        String redisKey = "token:" + token;
        redisTemplate.opsForValue().set(redisKey, user.getId().toString(), TOKEN_EXPIRE_SECONDS, TimeUnit.SECONDS);
        log.info("【Token 存入 Redis】userId={}, token={}, expireTime={}秒", user.getId(), token, TOKEN_EXPIRE_SECONDS);
        
        // 10. 清除登录失败记录（登录成功后）
        redisTemplate.delete(failCountKey);
        redisTemplate.delete(lockKey);
        log.info("【登录安全】清除登录失败记录：username={}", request.getUsername());
        
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

    /**
     * 增加登录失败次数并检查是否锁定账号
     */
    private void incrementLoginFailCount(String failCountKey, String lockKey) {
        // 增加失败次数
        Long failCount = redisTemplate.opsForValue().increment(failCountKey);
        
        // 如果是第一次失败，设置过期时间（5 分钟后自动清除）
        if (failCount == 1) {
            redisTemplate.expire(failCountKey, 5 * 60, TimeUnit.SECONDS);
        }
        
        // 检查是否超过最大失败次数
        if (failCount >= MAX_LOGIN_FAIL_COUNT) {
            // 锁定账号
            redisTemplate.opsForValue().set(lockKey, "LOCKED", LOCK_ACCOUNT_SECONDS, TimeUnit.SECONDS);
            log.warn("【登录安全】账号已被锁定：username={}, 失败次数={}, 锁定时间={}秒", 
                    failCountKey.replace("login:fail:", ""), failCount, LOCK_ACCOUNT_SECONDS);
        } else {
            log.warn("【登录安全】登录失败：username={}, 失败次数={}", 
                    failCountKey.replace("login:fail:", ""), failCount);
        }
    }

    /**
     * 修改密码
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void changePassword(Long userId, String oldPassword, String newPassword) {
        log.info("【修改密码】开始修改，userId={}", userId);
        
        // 1. 查询用户
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(404, "用户不存在");
        }
        
        // 2. 验证旧密码
        if (!PasswordUtil.verify(oldPassword, user.getPassword())) {
            log.warn("【修改密码】旧密码错误：userId={}", userId);
            throw new BusinessException(400, "原密码错误");
        }
        
        // 3. 校验新密码强度
        try {
            PasswordUtil.validateStrongPassword(newPassword);
        } catch (IllegalArgumentException e) {
            log.warn("【修改密码】新密码强度不足：userId={}, error={}", userId, e.getMessage());
            throw new BusinessException(400, e.getMessage());
        }
        
        // 4. 加密新密码并更新
        user.setPassword(PasswordUtil.encrypt(newPassword));
        boolean success = userMapper.updateById(user) > 0;
        
        if (success) {
            log.info("【修改密码成功】userId={}", userId);
        } else {
            log.error("【修改密码失败】userId={}", userId);
            throw new BusinessException(500, "修改密码失败");
        }
    }
}
