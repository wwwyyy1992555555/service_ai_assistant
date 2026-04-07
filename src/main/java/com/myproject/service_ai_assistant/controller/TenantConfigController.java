package com.myproject.service_ai_assistant.controller;

import com.myproject.service_ai_assistant.annotation.RequireRole;
import com.myproject.service_ai_assistant.common.Result;
import com.myproject.service_ai_assistant.common.LevelCode;
import com.myproject.service_ai_assistant.dto.TenantConfigDTO;
import com.myproject.service_ai_assistant.service.TenantConfigService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * 租户配置控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/settings")
@Tag(name = "租户配置管理")
public class TenantConfigController {

    @Autowired
    private TenantConfigService tenantConfigService;

    @GetMapping("/get")
    @Operation(summary = "获取系统配置", description = "获取当前租户的系统配置信息")
    public Result<TenantConfigDTO> getConfig(
            @Parameter(description = "租户 ID", required = true) @RequestParam Long tenantId
    ) {
        log.info("【获取系统配置】tenantId={}", tenantId);
        
        TenantConfigDTO config = tenantConfigService.getConfig(tenantId);
        
        log.info("【获取系统配置成功】tenantId={}", tenantId);
        return Result.success(config);
    }

    @PostMapping("/save")
    @RequireRole(minLevel = LevelCode.ROLE_LEVEL_ADMIN)
    @Operation(summary = "保存系统配置", description = "保存或更新系统配置信息")
    public Result<Boolean> saveConfig(
            @Parameter(description = "租户 ID", required = true) @RequestParam Long tenantId,
            @RequestBody TenantConfigDTO configDTO
    ) {
        log.info("【保存系统配置】tenantId={}, config={}", tenantId, configDTO);
        
        boolean success = tenantConfigService.saveConfig(tenantId, configDTO);
        
        if (success) {
            log.info("【保存系统配置成功】tenantId={}", tenantId);
            return Result.success(true);
        } else {
            log.error("【保存系统配置失败】tenantId={}", tenantId);
            return Result.error(500, "保存失败");
        }
    }
}
