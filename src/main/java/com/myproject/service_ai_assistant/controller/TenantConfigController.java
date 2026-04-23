package com.myproject.service_ai_assistant.controller;

import com.myproject.service_ai_assistant.common.Result;
import com.myproject.service_ai_assistant.dto.TenantConfigDTO;
import com.myproject.service_ai_assistant.service.TenantConfigService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

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

    /**
     * 获取系统配置（支持 tenantId）
     */
    @GetMapping("/get")
    @Operation(summary = "获取系统配置", description = "根据租户 ID 获取配置信息")
    public Result<Map<String, Object>> getConfig(@RequestParam Long tenantId) {
        log.info("【获取系统配置】tenantId={}", tenantId);
        
        if (tenantId == null || tenantId <= 0) {
            return Result.error(400, "租户 ID 不能为空");
        }
        
        // 获取配置
        TenantConfigDTO config = tenantConfigService.getConfig(tenantId);
        
        if (config == null) {
            log.warn("【获取系统配置失败】租户不存在：tenantId={}", tenantId);
            return Result.error(404, "租户不存在");
        }
        
        // 构建返回数据
        Map<String, Object> result = new HashMap<>();
        result.put("tenantId", tenantId);
        result.put("tenantName", config.getTenantName());
        result.put("welcomeMessage", config.getWelcomeMessage());
        result.put("themeColor", config.getThemeColor());
        result.put("serviceTime", config.getServiceTime());
        result.put("logoUrl", config.getLogoUrl());
        
        log.info("【获取系统配置成功】tenantId={}", tenantId);
        return Result.success(result);
    }

    @PutMapping("/save")
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
