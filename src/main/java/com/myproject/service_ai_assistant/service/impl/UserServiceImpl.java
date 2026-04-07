package com.myproject.service_ai_assistant.service.impl;

import cn.hutool.crypto.digest.BCrypt;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.myproject.service_ai_assistant.common.PasswordUtil;
import com.myproject.service_ai_assistant.common.LevelCode;
import com.myproject.service_ai_assistant.context.UserContext;
import com.myproject.service_ai_assistant.dto.LoginRequest;
import com.myproject.service_ai_assistant.dto.UserCreateRequest;
import com.myproject.service_ai_assistant.common.ResultCode;
import com.myproject.service_ai_assistant.dto.UserDTO;
import com.myproject.service_ai_assistant.entity.TenantInfo;
import com.myproject.service_ai_assistant.entity.User;
import com.myproject.service_ai_assistant.exception.BusinessException;
import com.myproject.service_ai_assistant.mapper.UserMapper;
import com.myproject.service_ai_assistant.service.TenantConfigService;
import com.myproject.service_ai_assistant.service.UserService;
import com.myproject.service_ai_assistant.mapper.TenantInfoMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 用户服务实现类
 */
@Slf4j
@Service
public class UserServiceImpl implements UserService {

    @Autowired
    private UserMapper userMapper;
    
    @Autowired
    private TenantInfoMapper tenantInfoMapper;
    
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
     * Redis Key 前缀：租户编码缓存（tenant_code -> tenant_id）
     */
    private static final String TENANT_CODE_CACHE_PREFIX = "tenant:code:";

    /**
     * 用户登录
     * 方案一：(tenant_id, username) 联合唯一
     */
    @Override
    public UserDTO login(LoginRequest request) {
        log.info("【用户登录】开始登录，username={}, tenantId={}, tenantCode={}", 
                request.getUsername(), request.getTenantId(), request.getTenantCode());
        
        // 1. 检查账号是否已被锁定
        String failCountKey = "login:fail:" + request.getUsername();
        String lockKey = "login:lock:" + request.getUsername();
        
        Boolean isLocked = redisTemplate.hasKey(lockKey);
        if (Boolean.TRUE.equals(isLocked)) {
            Long remainSeconds = redisTemplate.getExpire(lockKey, TimeUnit.SECONDS);
            log.warn("【用户登录】账号已被锁定，请稍后再试：username={}, 剩余时间={}秒", request.getUsername(), remainSeconds);
            throw new BusinessException(ResultCode.ACCOUNT_LOCKED, remainSeconds / 60);
        }
        
        // 2. 确定 tenantId（优先使用前端传的 tenantId，否则通过 tenantCode 解析）
        Long tenantId;
        if (request.getTenantId() != null && request.getTenantId() == LevelCode.ROLE_LEVEL_TENANT_ID) {
            // 超级管理员：tenant_id 固定为 0
            tenantId = LevelCode.ROLE_LEVEL_TENANT_ID;
            log.info("【用户登录】超级管理员登录模式：tenantId=0");
        } else if (request.getTenantCode() != null && !request.getTenantCode().trim().isEmpty()) {
            // 租户用户：根据 tenantCode 查 tenant_id
            String tenantCode = request.getTenantCode().trim();
            tenantId = getTenantIdByCode(tenantCode);
            log.info("【用户登录】租户登录模式：tenantCode={}, tenantId={}", tenantCode, tenantId);
        } else {
            log.warn("【用户登录】缺少租户信息：username={}", request.getUsername());
            throw new BusinessException(ResultCode.TENANT_CODE_REQUIRED);
        }
        
        // 3. 查询用户和租户配置（一次 SQL）
        java.util.Map<String, Object> userWithConfig = userMapper.selectUserWithConfig(tenantId, request.getUsername());
        
        if (userWithConfig == null) {
            log.warn("【用户登录】用户不存在：username={}, tenantId={}", request.getUsername(), tenantId);
            incrementLoginFailCount(failCountKey, lockKey);
            throw new BusinessException(ResultCode.AUTH_FAILED);
        }
        
        // 从 Map 中提取 User 对象
        User user = new User();
        user.setId(((Number) userWithConfig.get("id")).longValue());
        user.setTenantId(((Number) userWithConfig.get("tenant_id")).longValue());
        user.setUsername((String) userWithConfig.get("username"));
        user.setPassword((String) userWithConfig.get("password"));
        user.setRealName((String) userWithConfig.get("real_name"));
        user.setPhone((String) userWithConfig.get("phone"));
        user.setEmail((String) userWithConfig.get("email"));
        user.setRoleLevel(((Number) userWithConfig.get("role_level")).intValue());
        user.setStatus(((Number) userWithConfig.get("status")).intValue());
        user.setAvatarUrl((String) userWithConfig.get("avatar_url"));
        user.setLastLoginTime((java.time.LocalDateTime) userWithConfig.get("last_login_time"));
        user.setLastLoginIp((String) userWithConfig.get("last_login_ip"));
        user.setCreatedTime((java.time.LocalDateTime) userWithConfig.get("created_time"));
        user.setUpdatedTime((java.time.LocalDateTime) userWithConfig.get("updated_time"));
        
        log.info("【用户登录】用户已找到：userId={}, username={}, tenantId={}, roleLevel={}", 
                user.getId(), user.getUsername(), user.getTenantId(), user.getRoleLevel());
        
        // 4. 验证密码
        boolean passwordMatch = BCrypt.checkpw(request.getPassword(), user.getPassword());
        log.info("【用户登录】密码验证结果：{}", passwordMatch ? "匹配" : "不匹配");
        if (!passwordMatch) {
            log.warn("【用户登录】密码错误：username={}", request.getUsername());
            // 记录失败次数
            incrementLoginFailCount(failCountKey, lockKey);
            throw new BusinessException(ResultCode.AUTH_FAILED);
        }
        
        // 5. 检查用户状态
        if (user.getStatus() == 0) {
            log.warn("【用户登录】用户已被禁用：username={}", request.getUsername());
            throw new BusinessException(ResultCode.ACCOUNT_DISABLED);
        }
        
        // 6. 更新登录信息（局部事务）
        updateLoginInfo(user.getId());
        
        // 7. 转换为 DTO
        UserDTO userDTO = new UserDTO();
        BeanUtils.copyProperties(user, userDTO);
        // 不返回密码
        
        // 7.1 填充租户配置（从联合查询结果中获取）
        if (userWithConfig != null) {
            userDTO.setTenantLogoUrl((String) userWithConfig.get("tenant_logo_url"));
            userDTO.setTenantThemeColor((String) userWithConfig.get("tenant_theme_color"));
            userDTO.setTenantWelcomeMessage((String) userWithConfig.get("tenant_welcome_message"));
        }
        
        // 8. 填充租户信息
        if (user.getTenantId().equals(LevelCode.ROLE_LEVEL_SUPER_ADMIN)) {
            // 超级管理员
            log.info("【用户登录】超级管理员登录成功：username={}", request.getUsername());
            userDTO.setTenantId(LevelCode.ROLE_LEVEL_TENANT_ID);
            userDTO.setTenantName("平台管理端");
        } else {
            // 租户用户 - 从 tenant_info 获取租户名称
            try {
                TenantInfo tenantInfo = tenantInfoMapper.selectById(user.getTenantId());
                if (tenantInfo != null) {
                    userDTO.setTenantName(tenantInfo.getTenantName());
                }
            } catch (Exception e) {
                log.warn("【用户登录】获取租户名称失败：tenantId={}", user.getTenantId(), e);
            }
            log.info("【用户登录】租户用户登录成功：username={}, tenantId={}", request.getUsername(), user.getTenantId());
        }
        
        // 7. 生成 Token
        String token = generateToken(user);
        userDTO.setToken(token);
        
        // 9. 将 Token 和用户信息存入 Redis
        String redisKey = "token:" + token;
        // 存储格式：userId:tenantId:roleLevel（简化序列化，避免JSON依赖）
        String userInfo = user.getId() + ":" + user.getTenantId() + ":" + user.getRoleLevel();
        redisTemplate.opsForValue().set(redisKey, userInfo, TOKEN_EXPIRE_SECONDS, TimeUnit.SECONDS);
        log.info("【Token 存入 Redis】userId={}, tenantId={}, roleLevel={}, token={}", 
                user.getId(), user.getTenantId(), user.getRoleLevel(), token);
        
        // 10. 单设备登录控制
        String userTokenKey = USER_TOKEN_KEY_PREFIX + user.getId();
        String oldToken = redisTemplate.opsForValue().get(userTokenKey);
        if (oldToken != null) {
            // 删除旧 Token
            String oldRedisKey = "token:" + oldToken;
            redisTemplate.delete(oldRedisKey);
            log.info("【单设备登录】旧设备 Token 已失效：userId={}, oldToken={}", user.getId(), oldToken);
        }
        
        // 保存新 Token 映射
        redisTemplate.opsForValue().set(userTokenKey, token, TOKEN_EXPIRE_SECONDS, TimeUnit.SECONDS);
        log.info("【单设备登录】保存新 Token：userId={}, token={}", user.getId(), token);
        
        // 11. 清除登录失败记录（登录成功后）
        redisTemplate.delete(failCountKey);
        redisTemplate.delete(lockKey);
        log.info("【登录安全】清除登录失败记录：username={}", request.getUsername());
        
        return userDTO;
    }
    
    /**
     * 根据租户编码获取 tenant_id（带缓存）
     */
    private Long getTenantIdByCode(String tenantCode) {
        // 1. 先查缓存
        String cacheKey = TENANT_CODE_CACHE_PREFIX + tenantCode;
        String cachedId = redisTemplate.opsForValue().get(cacheKey);
        if (cachedId != null) {
            log.debug("【租户缓存命中】tenantCode={}, tenantId={}", tenantCode, cachedId);
            return Long.parseLong(cachedId);
        }
        
        // 2. 缓存未命中，查数据库
        TenantInfo tenant = tenantInfoMapper.selectOne(
            new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<TenantInfo>()
                .eq(TenantInfo::getTenantCode, tenantCode)
        );
        
        if (tenant == null) {
            log.warn("【用户登录】租户不存在：tenantCode={}", tenantCode);
            throw new BusinessException(ResultCode.TENANT_NOT_FOUND);
        }
        
        if (tenant.getStatus() == 0) {
            log.warn("【用户登录】租户已被禁用：tenantCode={}", tenantCode);
            throw new BusinessException(ResultCode.TENANT_DISABLED);
        }
        
        // 3. 写入缓存（TTL 30 天）
        redisTemplate.opsForValue().set(cacheKey, tenant.getId().toString(), 30, TimeUnit.DAYS);
        log.info("【租户缓存更新】tenantCode={}, tenantId={}", tenantCode, tenant.getId());
        
        return tenant.getId();
    }
    
    /**
     * 更新用户登录信息（局部事务）
     */
    @Transactional(rollbackFor = Exception.class)
    public void updateLoginInfo(Long userId) {
        User user = new User();
        user.setId(userId);
        user.setLastLoginTime(LocalDateTime.now());
        user.setLastLoginIp("127.0.0.1");
        userMapper.updateById(user);
    }

    @Override
    public UserDTO getUserById(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ResultCode.USER_NOT_FOUND);
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
            throw new BusinessException(ResultCode.USER_NOT_FOUND);
        }
        
        // 2. 验证旧密码
        if (!PasswordUtil.verify(oldPassword, user.getPassword())) {
            log.warn("【修改密码】旧密码错误：userId={}", userId);
            throw new BusinessException(ResultCode.OLD_PASSWORD_WRONG);
        }
        
        // 3. 校验新密码强度
        try {
            PasswordUtil.validateStrongPassword(newPassword);
        } catch (IllegalArgumentException e) {
            log.warn("【修改密码】新密码强度不足：userId={}, error={}", userId, e.getMessage());
            throw new BusinessException(ResultCode.PARAM_VALIDATION_ERROR, e.getMessage());
        }
        
        // 4. 加密新密码并更新
        user.setPassword(PasswordUtil.encrypt(newPassword));
        boolean success = userMapper.updateById(user) > 0;
        
        if (success) {
            log.info("【修改密码成功】userId={}", userId);
        } else {
            log.error("【修改密码失败】userId={}", userId);
            throw new BusinessException(ResultCode.PASSWORD_CHANGE_FAILED);
        }
    }

    /**
     * 创建用户
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public UserDTO createUser(UserCreateRequest request) {
        log.info("【创建用户】开始创建，username={}, tenantId={}", request.getUsername(), request.getTenantId());
        
        // 1. 检查用户名是否已存在（MyBatis-Plus 的 @TableLogic 会自动添加 deleted=0 条件）
        User existingUser = userMapper.selectByUsername(request.getTenantId(), request.getUsername());
        
        if (existingUser != null) {
            log.warn("【创建用户】用户名已存在：username={}, tenantId={}", request.getUsername(), request.getTenantId());
            throw new BusinessException(ResultCode.USERNAME_EXISTS);
        }
        
        // 1.1 物理删除该用户名下的所有已删除记录（使用原生SQL绕过@TableLogic）
        List<User> allUsers = userMapper.selectAllByUsernameIncludingDeleted(request.getTenantId(), request.getUsername());
        List<User> deletedUsers = allUsers.stream()
            .filter(u -> u.getDeleted() != null && u.getDeleted() == 1)
            .collect(Collectors.toList());
        
        if (!deletedUsers.isEmpty()) {
            log.info("【创建用户】清理历史已删除记录：username={}, count={}", request.getUsername(), deletedUsers.size());
            for (User deletedUser : deletedUsers) {
                userMapper.physicallyDeleteById(deletedUser.getId());
            }
        }
        
        // 2. 检查手机号是否已存在
        if (request.getPhone() != null && !request.getPhone().isEmpty()) {
            User existingPhoneUser = userMapper.selectByPhone(request.getTenantId(), request.getPhone());
            if (existingPhoneUser != null) {
                log.warn("【创建用户】手机号已存在：phone={}, tenantId={}", request.getPhone(), request.getTenantId());
                throw new BusinessException(ResultCode.PHONE_EXISTS);
            }
        }
        
        // 3. 校验密码强度
        try {
            PasswordUtil.validateStrongPassword(request.getPassword());
        } catch (IllegalArgumentException e) {
            log.warn("【创建用户】密码强度不足：username={}, error={}", request.getUsername(), e.getMessage());
            throw new BusinessException(ResultCode.PARAM_VALIDATION_ERROR, e.getMessage());
        }
        
        // 4. 创建用户
        User user = new User();
        user.setTenantId(request.getTenantId());
        user.setUsername(request.getUsername());
        user.setPassword(PasswordUtil.encrypt(request.getPassword()));
        user.setRealName(request.getRealName());
        user.setPhone(request.getPhone());
        user.setEmail(request.getEmail());
        user.setRoleLevel(request.getRoleLevel());
        user.setStatus(request.getStatus());
        user.setCreatedTime(LocalDateTime.now());
        user.setUpdatedTime(LocalDateTime.now());
        
        int result = userMapper.insert(user);
        if (result <= 0) {
            log.error("【创建用户】失败：username={}, tenantId={}", request.getUsername(), request.getTenantId());
            throw new BusinessException(ResultCode.USER_CREATE_FAILED);
        }
        
        log.info("【创建用户成功】userId={}, username={}, tenantId={}", user.getId(), user.getUsername(), user.getTenantId());
        
        // 5. 转换为 DTO
        UserDTO userDTO = new UserDTO();
        BeanUtils.copyProperties(user, userDTO);
        // 不返回密码
        
        return userDTO;
    }

    /**
     * 重置用户密码
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void resetPassword(Long userId, String newPassword) {
        log.info("【重置密码】开始重置，userId={}", userId);
        
        // 1. 查询用户
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ResultCode.USER_NOT_FOUND);
        }
        
        // ✅ 水平越权校验：确保操作的是当前租户的数据（运营商 tenant_id=0 可跨租户操作）
        UserContext.validateHorizontalPermission(user.getTenantId());
        
        // 2. 校验新密码强度
        try {
            PasswordUtil.validateStrongPassword(newPassword);
        } catch (IllegalArgumentException e) {
            log.warn("【重置密码】新密码强度不足：userId={}, error={}", userId, e.getMessage());
            throw new BusinessException(ResultCode.PARAM_VALIDATION_ERROR, e.getMessage());
        }
        
        // 3. 加密新密码并更新
        user.setPassword(PasswordUtil.encrypt(newPassword));
        user.setUpdatedTime(LocalDateTime.now());
        boolean success = userMapper.updateById(user) > 0;
        
        if (success) {
            log.info("【重置密码成功】userId={}", userId);
        } else {
            log.error("【重置密码失败】userId={}", userId);
            throw new BusinessException(ResultCode.PASSWORD_RESET_FAILED);
        }
    }

    /**
     * 禁用/启用用户
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateUserStatus(Long userId, Integer status) {
        log.info("【更新用户状态】开始更新，userId={}, status={}", userId, status);
        
        // 1. 查询用户
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ResultCode.USER_NOT_FOUND);
        }
        
        // ✅ 水平越权校验：确保操作的是当前租户的数据（运营商 tenant_id=0 可跨租户操作）
        UserContext.validateHorizontalPermission(user.getTenantId());
        
        // 2. 更新状态
        user.setStatus(status);
        user.setUpdatedTime(LocalDateTime.now());
        boolean success = userMapper.updateById(user) > 0;
        
        if (success) {
            log.info("【更新用户状态成功】userId={}, status={}", userId, status);
        } else {
            log.error("【更新用户状态失败】userId={}, status={}", userId, status);
            throw new BusinessException(ResultCode.USER_UPDATE_FAILED);
        }
    }
    
    /**
     * 删除用户
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteUser(Long userId) {
        log.info("【删除用户】开始：userId={}", userId);
        
        // 1. 查询用户
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ResultCode.USER_NOT_FOUND);
        }
        
        // ✅ 水平越权校验：确保操作的是当前租户的数据（运营商 tenant_id=0 可跨租户操作）
        UserContext.validateHorizontalPermission(user.getTenantId());
        
        // 2. 不允许删除运营商（tenant_id=0）
        if (user.getRoleLevel() != null && user.getTenantId() == LevelCode.ROLE_LEVEL_TENANT_ID) {
            log.warn("【删除用户】不允许删除超级管理员：userId={}, username={}", userId, user.getUsername());
            throw new BusinessException(ResultCode.DELETE_SUPER_ADMIN_FORBIDDEN);
        }
        
        // 2.1 物理删除该用户名下的所有已删除记录（使用原生SQL绕过@TableLogic）
        List<User> allUsers = userMapper.selectAllByUsernameIncludingDeleted(user.getTenantId(), user.getUsername());
        List<User> deletedUsers = allUsers.stream()
            .filter(u -> u.getDeleted() != null && u.getDeleted() == 1)
            .collect(Collectors.toList());
        
        if (!deletedUsers.isEmpty()) {
            log.info("【删除用户】清理历史已删除记录：username={}, count={}", user.getUsername(), deletedUsers.size());
            for (User deletedUser : deletedUsers) {
                userMapper.physicallyDeleteById(deletedUser.getId());
            }
        }
        
        // 3. 执行逻辑删除
        boolean success = userMapper.deleteById(userId) > 0;
        
        if (success) {
            log.info("【删除用户】成功：userId={}, username={}", userId, user.getUsername());
            
            // 4. 清理该用户的 Redis Token（如果存在）
            try {
                String userTokenKey = USER_TOKEN_KEY_PREFIX + userId;
                String token = redisTemplate.opsForValue().get(userTokenKey);
                if (token != null) {
                    // 删除用户 Token 映射
                    redisTemplate.delete(userTokenKey);
                    // 删除 Token 数据
                    String redisKey = "token:" + token;
                    redisTemplate.delete(redisKey);
                    log.info("【删除用户】已清理用户 Token：userId={}, token={}", userId, token);
                }
            } catch (Exception e) {
                log.error("【删除用户】清理 Token 失败：userId={}", userId, e);
                // Token 清理失败不影响删除操作，继续执行
            }
        } else {
            log.error("【删除用户】失败：userId={}", userId);
            throw new BusinessException(ResultCode.USER_DELETE_FAILED);
        }
    }

    /**
     * 根据租户 ID 获取用户列表（支持分页和搜索）
     */
    @Override
    public Page<UserDTO> getUsersByTenantId(Long tenantId, Integer current, Integer size, String keyword, Integer currentUserRoleLevel) {
        log.info("【获取用户列表】开始查询，tenantId={}, current={}, size={}, keyword={}, currentUserRoleLevel={}", 
                tenantId, current, size, keyword, currentUserRoleLevel);
        
        // 权限校验：操作员(roleLevel=2)无权访问用户管理
        if (currentUserRoleLevel != null && currentUserRoleLevel > 1) {
            log.warn("【获取用户列表】权限不足：currentUserRoleLevel={}", currentUserRoleLevel);
            throw new BusinessException(ResultCode.PERMISSION_DENIED);
        }
        
        // 创建 MyBatis-Plus 的 Page 对象
        Page<User> userPage = new Page<>(current, size);
        
        // 使用 MyBatis-Plus 的分页查询
        Page<User> resultPage;
        if (keyword != null && !keyword.trim().isEmpty()) {
            // 有搜索关键词时，使用搜索方法
            List<User> users = userMapper.searchByTenantId(tenantId, keyword.trim());
            
            // 权限过滤：普通管理员(roleLevel=1)只能查看一级(1)和操作员(2)
            if (currentUserRoleLevel != null && currentUserRoleLevel.equals(LevelCode.ROLE_LEVEL_ADMIN)) {
                users = users.stream()
                    .filter(user -> user.getRoleLevel() != null && user.getRoleLevel() >= 1)
                    .collect(Collectors.toList());
            }
            // 超级管理员(roleLevel=0)无需过滤，查看所有用户
            
            // 手动分页
            int total = users.size();
            int fromIndex = (current - 1) * size;
            int toIndex = Math.min(fromIndex + size, total);
            List<User> pagedUsers = fromIndex < total ? users.subList(fromIndex, toIndex) : new ArrayList<>();
            
            resultPage = new Page<>(current, size, total);
            resultPage.setRecords(pagedUsers);
        } else {
            // 无搜索关键词时，使用 MyBatis-Plus 的 selectPage
            LambdaQueryWrapper<User> queryWrapper = new LambdaQueryWrapper<>();
            
            // 超级管理员(tenantId=0)查询所有租户的用户，否则只查询指定租户
            if (tenantId != null && tenantId > LevelCode.ROLE_LEVEL_SUPER_ADMIN) {
                queryWrapper.eq(User::getTenantId, tenantId);
            }
            
            // 权限过滤：普通管理员(roleLevel=1)只能查看一级(1)和操作员(2)
            if (currentUserRoleLevel != null && currentUserRoleLevel.equals(LevelCode.ROLE_LEVEL_ADMIN)) {
                queryWrapper.ge(User::getRoleLevel, 1);
            }
            // 超级管理员(roleLevel=0)无需过滤，查看所有用户
            
            resultPage = userMapper.selectPage(userPage, queryWrapper);
        }
        
        // 转换为 UserDTO
        Page<UserDTO> dtoPage = new Page<>(resultPage.getCurrent(), resultPage.getSize(), resultPage.getTotal());
        List<UserDTO> userDTOs = new ArrayList<>();
        
        for (User user : resultPage.getRecords()) {
            UserDTO userDTO = new UserDTO();
            BeanUtils.copyProperties(user, userDTO);
            // 不返回密码
            
            // 查询租户信息（名称和编码）
            if (user.getTenantId() != null && user.getTenantId() > LevelCode.ROLE_LEVEL_SUPER_ADMIN) {
                try {
                    TenantInfo tenantInfo = tenantInfoMapper.selectById(user.getTenantId());
                    if (tenantInfo != null) {
                        userDTO.setTenantName(tenantInfo.getTenantName());
                        userDTO.setTenantCode(tenantInfo.getTenantCode());
                    } else {
                        userDTO.setTenantName("租户 " + user.getTenantId());
                    }
                } catch (Exception e) {
                    log.warn("获取租户信息失败：tenantId={}", user.getTenantId(), e);
                    userDTO.setTenantName("租户 " + user.getTenantId());
                }
            }
            
            userDTOs.add(userDTO);
        }
        
        dtoPage.setRecords(userDTOs);
        log.info("【获取用户列表】查询完成，tenantId={}, total={}, size={}", tenantId, dtoPage.getTotal(), dtoPage.getSize());
        return dtoPage;
    }
    
    /**
     * 检查用户名在租户下是否已存在
     */
    @Override
    public boolean isUsernameExists(Long tenantId, String username) {
        log.info("【检查用户名】tenantId={}, username={}", tenantId, username);
        User existingUser = userMapper.selectByUsername(tenantId, username);
        boolean exists = existingUser != null;
        log.info("【检查用户名】结果：{}", exists ? "已存在" : "不存在");
        return exists;
    }
}