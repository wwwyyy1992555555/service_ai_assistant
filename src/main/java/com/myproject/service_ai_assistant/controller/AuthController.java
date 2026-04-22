package com.myproject.service_ai_assistant.controller;

import com.myproject.service_ai_assistant.common.Result;
import com.myproject.service_ai_assistant.common.ResultCode;
import com.myproject.service_ai_assistant.dto.LoginRequest;
import com.myproject.service_ai_assistant.dto.ResetPasswordRequest;
import com.myproject.service_ai_assistant.dto.ResetPasswordWithUsernameRequest;
import com.myproject.service_ai_assistant.dto.SendVerifyCodeRequest;
import com.myproject.service_ai_assistant.dto.UserDTO;
import com.myproject.service_ai_assistant.entity.User;
import com.myproject.service_ai_assistant.exception.BusinessException;
import com.myproject.service_ai_assistant.mapper.UserMapper;
import com.myproject.service_ai_assistant.service.EmailService;
import com.myproject.service_ai_assistant.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * 认证控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/auth")
@Tag(name = "认证管理")
public class AuthController {

    @Autowired
    private UserService userService;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private com.myproject.service_ai_assistant.mapper.TenantInfoMapper tenantInfoMapper;

    @Autowired
    private EmailService emailService;

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    /**
     * 用户登录
     */
    @PostMapping("/login")
    @Operation(summary = "用户登录", description = "用户名密码登录")
    public Result<UserDTO> login(
            @RequestBody @Validated LoginRequest request,
            HttpServletRequest httpRequest
    ) {
        log.info("【用户登录】username={}, ip={}", request.getUsername(), httpRequest.getRemoteAddr());

        UserDTO userDTO = userService.login(request);

        log.info("【用户登录成功】username={}, userId={}", request.getUsername(), userDTO.getId());
        return Result.success(userDTO);
    }

    /**
     * 用户登出
     */
    @PostMapping("/logout")
    @Operation(summary = "用户登出", description = "退出登录")
    public Result<Boolean> logout(
            @Parameter(description = "用户 ID", required = true) @RequestParam Long userId
    ) {
        log.info("【用户登出】userId={}", userId);
        // TODO: 实现 token 失效逻辑
        return Result.success(true);
    }

    /**
     * 获取当前用户信息
     */
    @GetMapping("/info")
    @Operation(summary = "获取用户信息", description = "获取当前登录用户的信息")
    public Result<UserDTO> getUserInfo(
            @Parameter(description = "用户 ID", required = true) @RequestParam Long userId
    ) {
        log.info("【获取用户信息】userId={}", userId);
        UserDTO userDTO = userService.getUserById(userId);
        return Result.success(userDTO);
    }

    /**
     * 发送重置密码验证码
     */
    @PostMapping("/forgot-password/send-code")
    @Operation(summary = "发送重置密码验证码", description = "通过租户联系邮箱发送验证码")
    public Result<String> sendResetPasswordCode(
            @RequestBody SendVerifyCodeRequest request
    ) {
        log.info("【发送重置密码验证码】开始：email={}", request.getContactEmail());
        
        // 1. 查询租户是否存在（通过联系邮箱）
        com.myproject.service_ai_assistant.entity.TenantInfo tenant = 
            tenantInfoMapper.selectOne(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<com.myproject.service_ai_assistant.entity.TenantInfo>()
                    .eq(com.myproject.service_ai_assistant.entity.TenantInfo::getContactEmail, request.getContactEmail().toLowerCase())
                    .eq(com.myproject.service_ai_assistant.entity.TenantInfo::getDeleted, 0)
                    .last("LIMIT 1")
            );
        
        if (tenant == null) {
            log.warn("【发送重置密码验证码】租户不存在：email={}", request.getContactEmail());
            throw new BusinessException(ResultCode.TENANT_NOT_FOUND);
        }
        
        // 2. 生成验证码
        String verifyCode = String.format("%06d", (int) (Math.random() * 1000000));
        
        // 3. 存储到 Redis（5 分钟有效期）
        String redisKey = "reset:password:" + request.getContactEmail();
        try {
            redisTemplate.opsForValue().set(redisKey, verifyCode, 5, TimeUnit.MINUTES);
            log.info("【发送重置密码验证码】验证码已存入Redis：key={}", redisKey);
        } catch (Exception e) {
            log.error("【发送重置密码验证码】Redis存储失败", e);
            throw new BusinessException(ResultCode.REDIS_OPERATION_FAILED);
        }
        
        // 4. 发送邮件
        try {
            emailService.sendResetPasswordEmail(request.getContactEmail(), verifyCode);
            log.info("【发送重置密码验证码】邮件发送成功：email={}, code={}", request.getContactEmail(), verifyCode);
        } catch (Exception e) {
            log.error("【发送重置密码验证码】邮件发送失败", e);
            // 删除 Redis 中的验证码
            redisTemplate.delete(redisKey);
            throw new BusinessException(ResultCode.EMAIL_SEND_FAILED);
        }
        
        return Result.success("验证码已发送至您的邮箱");
    }

    /**
     * 重置密码
     */
    @PostMapping("/forgot-password/reset")
    @Operation(summary = "重置密码", description = "通过租户联系邮箱验证码重置管理员密码")
    public Result<Boolean> resetPassword(
            @RequestBody ResetPasswordWithUsernameRequest request
    ) {
        log.info("【重置密码】开始：email={}, username={}", request.getEmail(), request.getUsername());
        
        // 1. 验证验证码
        String redisKey = "reset:password:" + request.getEmail();
        String storedCode = redisTemplate.opsForValue().get(redisKey);
        
        if (storedCode == null) {
            log.warn("【重置密码】验证码已过期：email={}", request.getEmail());
            throw new BusinessException(ResultCode.VERIFY_CODE_EXPIRED);
        }
        
        if (!storedCode.equals(request.getVerifyCode())) {
            log.warn("【重置密码】验证码错误：email={}, input={}", request.getEmail(), request.getVerifyCode());
            throw new BusinessException(ResultCode.VERIFY_CODE_ERROR);
        }
        
        // 2. 查询租户
        com.myproject.service_ai_assistant.entity.TenantInfo tenant = 
            tenantInfoMapper.selectOne(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<com.myproject.service_ai_assistant.entity.TenantInfo>()
                    .eq(com.myproject.service_ai_assistant.entity.TenantInfo::getContactEmail, request.getEmail().toLowerCase())
                    .eq(com.myproject.service_ai_assistant.entity.TenantInfo::getDeleted, 0)
                    .last("LIMIT 1")
            );
        
        if (tenant == null) {
            log.warn("【重置密码】租户不存在：email={}", request.getEmail());
            throw new BusinessException(ResultCode.TENANT_NOT_FOUND);
        }
        
        // 3. 查询指定用户名的管理员账号
        User adminUser = userMapper.selectOne(
            new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<User>()
                .eq(User::getTenantId, tenant.getId())
                .eq(User::getUsername, request.getUsername())
                .eq(User::getRoleLevel, com.myproject.service_ai_assistant.common.LevelCode.ROLE_LEVEL_ADMIN)
                .eq(User::getDeleted, 0)
        );
        
        if (adminUser == null) {
            log.warn("【重置密码】管理员账号不存在：tenantId={}, username={}", tenant.getId(), request.getUsername());
            throw new BusinessException(ResultCode.USER_NOT_FOUND);
        }
        
        // 4. 更新密码
        try {
            com.myproject.service_ai_assistant.common.PasswordUtil.validateStrongPassword(request.getNewPassword());
        } catch (IllegalArgumentException e) {
            log.warn("【重置密码】新密码强度不足：email={}, error={}", request.getEmail(), e.getMessage());
            throw new BusinessException(ResultCode.PARAM_VALIDATION_ERROR, e.getMessage());
        }
        
        adminUser.setPassword(com.myproject.service_ai_assistant.common.PasswordUtil.encrypt(request.getNewPassword()));
        adminUser.setUpdatedTime(java.time.LocalDateTime.now());
        boolean success = userMapper.updateById(adminUser) > 0;
        
        if (!success) {
            log.error("【重置密码】更新失败：email={}, username={}", request.getEmail(), request.getUsername());
            throw new BusinessException(ResultCode.PASSWORD_RESET_FAILED);
        }
        
        // 5. 删除验证码
        redisTemplate.delete(redisKey);
        
        log.info("【重置密码】成功：email={}, tenantId={}, username={}, userId={}", 
                request.getEmail(), tenant.getId(), request.getUsername(), adminUser.getId());
        return Result.success(true);
    }
}
