package com.myproject.service_ai_assistant.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.myproject.service_ai_assistant.common.Result;
import com.myproject.service_ai_assistant.entity.TenantInfo;
import com.myproject.service_ai_assistant.service.TenantInfoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * 租户管理控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/tenant")
@Tag(name = "租户管理")
public class TenantInfoController {

    @Autowired
    private TenantInfoService tenantInfoService;

    /**
     * 获取租户列表（分页）
     */
    @GetMapping("/list")
    @Operation(summary = "获取租户列表", description = "分页查询租户列表，支持搜索和筛选")
    public Result<Page<TenantInfo>> getTenantList(
            @Parameter(description = "当前页码", required = false, example = "1") @RequestParam(defaultValue = "1") Integer current,
            @Parameter(description = "每页大小", required = false, example = "10") @RequestParam(defaultValue = "10") Integer size,
            @Parameter(description = "搜索关键词", required = false) @RequestParam(required = false) String keyword,
            @Parameter(description = "状态：0-禁用 1-启用", required = false) @RequestParam(required = false) Integer status,
            @Parameter(description = "行业类型 ID", required = false) @RequestParam(required = false) Integer industryType
    ) {
        log.info("【获取租户列表】current={}, size={}, keyword={}, status={}, industryType={}", current, size, keyword, status, industryType);
        try {
            Page<TenantInfo> page = tenantInfoService.getTenantList(current, size, keyword, status, industryType);
            log.info("【获取租户列表成功】total={}", page.getTotal());
            return Result.success(page);
        } catch (Exception e) {
            log.error("【获取租户列表失败】{}", e.getMessage(), e);
            throw e;
        }
    }

    /**
     * 创建租户
     */
    @PostMapping("/create")
    @Operation(summary = "创建租户", description = "创建新的租户")
    public Result<TenantInfo> createTenant(
            @Validated @RequestBody TenantInfo tenantInfo
    ) {
        log.info("【创建租户】请求参数：tenantCode={}, tenantName={}", tenantInfo.getTenantCode(), tenantInfo.getTenantName());
        try {
            TenantInfo result = tenantInfoService.createTenant(tenantInfo);
            log.info("【创建租户成功】id={}, tenantCode={}", result.getId(), result.getTenantCode());
            return Result.success(result);
        } catch (Exception e) {
            log.error("【创建租户失败】{}", e.getMessage(), e);
            throw e;
        }
    }

    /**
     * 删除租户
     */
    @PostMapping("/delete")
    @Operation(summary = "删除租户", description = "删除指定租户（只能删除已禁用的租户）")
    public Result<Boolean> deleteTenant(
            @Parameter(description = "租户 ID", required = true) @RequestParam Long tenantId
    ) {
        log.info("【删除租户】tenantId={}", tenantId);
        try {
            tenantInfoService.deleteTenant(tenantId);
            log.info("【删除租户成功】tenantId={}", tenantId);
            return Result.success(true);
        } catch (Exception e) {
            log.error("【删除租户失败】{}", e.getMessage(), e);
            throw e;
        }
    }

    /**
     * 更新租户状态
     */
    @PostMapping("/update-status")
    @Operation(summary = "更新租户状态", description = "启用或禁用租户")
    public Result<Boolean> updateTenantStatus(
            @Parameter(description = "租户 ID", required = true) @RequestParam Long tenantId,
            @Parameter(description = "状态：0-禁用 1-启用", required = true) @RequestParam Integer status
    ) {
        log.info("【更新租户状态】tenantId={}, status={}", tenantId, status);
        try {
            tenantInfoService.updateTenantStatus(tenantId, status);
            log.info("【更新租户状态成功】tenantId={}, status={}", tenantId, status);
            return Result.success(true);
        } catch (Exception e) {
            log.error("【更新租户状态失败】{}", e.getMessage(), e);
            throw e;
        }
    }

    /**
     * 获取租户详情
     */
    @GetMapping("/{id}")
    @Operation(summary = "获取租户详情", description = "根据ID获取租户详细信息")
    public Result<TenantInfo> getTenantById(
            @Parameter(description = "租户 ID", required = true) @PathVariable Long id
    ) {
        log.info("【获取租户详情】id={}", id);
        try {
            TenantInfo tenant = tenantInfoService.getTenantById(id);
            if (tenant == null) {
                return Result.error(404, "租户不存在");
            }
            return Result.success(tenant);
        } catch (Exception e) {
            log.error("【获取租户详情失败】{}", e.getMessage(), e);
            throw e;
        }
    }

    /**
     * 更新租户信息
     */
    @PostMapping("/update")
    @Operation(summary = "更新租户信息", description = "修改租户基本信息")
    public Result<TenantInfo> updateTenant(
            @Validated @RequestBody TenantInfo tenantInfo
    ) {
        log.info("【更新租户信息】id={}, tenantCode={}", tenantInfo.getId(), tenantInfo.getTenantCode());
        try {
            tenantInfoService.updateTenant(tenantInfo);
            // 重新查询最新数据返回
            TenantInfo updated = tenantInfoService.getTenantById(tenantInfo.getId());
            return Result.success(updated);
        } catch (Exception e) {
            log.error("【更新租户信息失败】{}", e.getMessage(), e);
            throw e;
        }
    }
}
