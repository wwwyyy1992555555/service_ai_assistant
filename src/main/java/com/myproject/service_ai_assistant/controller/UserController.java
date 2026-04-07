package com.myproject.service_ai_assistant.controller;

import com.myproject.service_ai_assistant.annotation.RequireRole;
import com.myproject.service_ai_assistant.common.Result;
import com.myproject.service_ai_assistant.common.LevelCode;
import com.myproject.service_ai_assistant.dto.UserCreateRequest;
import com.myproject.service_ai_assistant.dto.UserDTO;
import com.myproject.service_ai_assistant.entity.TenantInfo;
import com.myproject.service_ai_assistant.mapper.TenantInfoMapper;
import com.myproject.service_ai_assistant.service.UserService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 用户管理控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/user")
@Tag(name = "用户管理")
public class UserController {

    @Autowired
    private UserService userService;

    @Autowired
    private TenantInfoMapper tenantInfoMapper;

    /**
     * 创建用户（需要管理员权限）
     */
    @PostMapping("/create")
    @RequireRole(minLevel = LevelCode.ROLE_LEVEL_ADMIN)
    @Operation(summary = "创建用户", description = "租户管理员创建用户")
    public Result<UserDTO> createUser(
            @Validated @RequestBody UserCreateRequest request
    ) {
        log.info("【创建用户】请求参数：username={}, tenantId={}", request.getUsername(), request.getTenantId());
        try {
            UserDTO userDTO = userService.createUser(request);
            log.info("【创建用户】成功：userId={}, username={}", userDTO.getId(), userDTO.getUsername());
            return Result.success(userDTO);
        } catch (Exception e) {
            log.error("【创建用户】失败：{}", e.getMessage(), e);
            throw e;
        }
    }

    /**
     * 重置用户密码（需要管理员权限）
     */
    @PostMapping("/reset-password")
    @RequireRole(minLevel = LevelCode.ROLE_LEVEL_ADMIN)
    @Operation(summary = "重置用户密码", description = "管理员重置用户密码")
    public Result<Boolean> resetPassword(
            @Parameter(description = "用户 ID", required = true) @RequestParam Long userId,
            @Parameter(description = "新密码", required = true) @RequestParam String newPassword
    ) {
        log.info("【重置密码】开始：userId={}", userId);
        try {
            userService.resetPassword(userId, newPassword);
            log.info("【重置密码】成功：userId={}", userId);
            return Result.success(true);
        } catch (Exception e) {
            log.error("【重置密码】失败：{}", e.getMessage(), e);
            throw e;
        }
    }

    /**
     * 更新用户状态（需要管理员权限）
     */
    @PostMapping("/update-status")
    @RequireRole(minLevel = LevelCode.ROLE_LEVEL_ADMIN)
    @Operation(summary = "更新用户状态", description = "启用/禁用用户")
    public ResponseEntity<?> updateUserStatus(
            @Parameter(description = "用户 ID", required = true) @RequestParam Long userId,
            @Parameter(description = "状态（1-启用，0-禁用）", required = true) @RequestParam Integer status
    ) {
        log.info("【更新用户状态】开始：userId={}, status={}", userId, status);
        try {
            userService.updateUserStatus(userId, status);
            log.info("【更新用户状态】成功：userId={}, status={}", userId, status);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            log.error("【更新用户状态】失败：{}", e.getMessage(), e);
            throw e;
        }
    }

    /**
     * 获取用户信息
     */
    @GetMapping("/info")
    @Operation(summary = "获取用户信息", description = "根据用户ID获取用户信息")
    public ResponseEntity<?> getUserInfo(
            @Parameter(description = "用户 ID", required = true) @RequestParam Long userId
    ) {
        log.info("【获取用户信息】开始：userId={}", userId);
        try {
            UserDTO userDTO = userService.getUserById(userId);
            log.info("【获取用户信息】成功：userId={}, username={}", userDTO.getId(), userDTO.getUsername());
            return ResponseEntity.ok(userDTO);
        } catch (Exception e) {
            log.error("【获取用户信息】失败：{}", e.getMessage(), e);
            throw e;
        }
    }

    /**
     * 获取租户用户列表
     */
    @GetMapping("/list")
    @Operation(summary = "获取租户用户列表", description = "根据租户 ID 获取用户列表（支持分页和搜索）")
    public Result<Page<UserDTO>> getUsersByTenantId(
            @Parameter(description = "租户 ID", required = true) @RequestParam Long tenantId,
            @Parameter(description = "当前页码", required = false, example = "1") @RequestParam(defaultValue = "1") Integer current,
            @Parameter(description = "每页大小", required = false, example = "10") @RequestParam(defaultValue = "10") Integer size,
            @Parameter(description = "搜索关键词", required = false) @RequestParam(required = false) String keyword,
            @Parameter(description = "当前用户角色级别", required = false) @RequestParam(required = false) Integer currentUserRoleLevel
    ) {
        log.info("【获取租户用户列表】开始：tenantId={}, current={}, size={}, keyword={}, currentUserRoleLevel={}", 
                tenantId, current, size, keyword, currentUserRoleLevel);
        try {
            Page<UserDTO> page = userService.getUsersByTenantId(tenantId, current, size, keyword, currentUserRoleLevel);
            log.info("【获取租户用户列表】成功：tenantId={}, total={}", tenantId, page.getTotal());
            return Result.success(page);
        } catch (Exception e) {
            log.error("【获取租户用户列表】失败：{}", e.getMessage(), e);
            throw e;
        }
    }
    
    /**
     * 删除用户（需要管理员权限）
     */
    @PostMapping("/delete")
    @RequireRole(minLevel = LevelCode.ROLE_LEVEL_ADMIN)
    @Operation(summary = "删除用户", description = "删除指定用户")
    public Result<Boolean> deleteUser(
            @Parameter(description = "用户 ID", required = true) @RequestParam Long userId
    ) {
        log.info("【删除用户】开始：userId={}", userId);
        try {
            userService.deleteUser(userId);
            log.info("【删除用户】成功：userId={}", userId);
            return Result.success(true);
        } catch (Exception e) {
            log.error("【删除用户】失败：{}", e.getMessage(), e);
            throw e;
        }
    }
    
    /**
     * 检查用户名是否存在
     */
    @GetMapping("/check-username")
    @Operation(summary = "检查用户名是否存在", description = "检查指定租户下用户名是否已存在")
    public Result<Boolean> checkUsernameExists(
            @Parameter(description = "租户 ID", required = true) @RequestParam Long tenantId,
            @Parameter(description = "用户名", required = true) @RequestParam String username
    ) {
        log.info("【检查用户名】tenantId={}, username={}", tenantId, username);
        try {
            boolean exists = userService.isUsernameExists(tenantId, username);
            log.info("【检查用户名】结果：{}", exists ? "已存在" : "不存在");
            return Result.success(exists);
        } catch (Exception e) {
            log.error("【检查用户名】失败：{}", e.getMessage(), e);
            throw e;
        }
    }

    /**
     * 搜索租户（用于新建用户时选择）
     */
    @GetMapping("/search-tenants")
    @Operation(summary = "搜索租户", description = "根据关键词搜索有效的租户列表")
    public Result<List<Map<String, Object>>> searchTenants(
            @Parameter(description = "搜索关键词") @RequestParam(required = false) String keyword
    ) {
        log.info("【搜索租户】keyword={}", keyword);
        try {
            LambdaQueryWrapper<TenantInfo> wrapper = new LambdaQueryWrapper<>();
            if (keyword != null && !keyword.isEmpty()) {
                wrapper.and(w -> w.like(TenantInfo::getTenantCode, keyword)
                        .or().like(TenantInfo::getTenantName, keyword));
            }
            wrapper.eq(TenantInfo::getStatus, 1) // 只查询启用的
                   .orderByDesc(TenantInfo::getCreatedTime)
                   .last("LIMIT 20"); // 限制返回数量
            
            List<TenantInfo> tenants = tenantInfoMapper.selectList(wrapper);
            List<Map<String, Object>> result = tenants.stream().map(t -> {
                Map<String, Object> map = new HashMap<>();
                map.put("id", t.getId());
                map.put("tenantCode", t.getTenantCode());
                map.put("tenantName", t.getTenantName());
                return map;
            }).collect(Collectors.toList());
            
            return Result.success(result);
        } catch (Exception e) {
            log.error("【搜索租户】失败：{}", e.getMessage(), e);
            throw e;
        }
    }
}