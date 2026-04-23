package com.myproject.service_ai_assistant.controller;

import com.myproject.service_ai_assistant.common.Result;
import com.myproject.service_ai_assistant.entity.TenantInfo;
import com.myproject.service_ai_assistant.service.TenantConfigService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * Chat 租户验证控制器 - 验证 tenant_code 并返回 tenantId
 */
@Slf4j
@RestController
@RequestMapping("/api/chat")
@Tag(name = "租户验证")
public class ChatAccessController {

    @Autowired
    private TenantConfigService tenantConfigService;

    @PostMapping("/init")
    @Operation(summary = "租户验证", description = "验证 tenant_code 有效性并返回 tenantId")
    public Result<Map<String, Object>> initChat(@RequestBody Map<String, String> request) {
        String tenantCode = request.get("tenantCode");
        log.info("【租户验证】tenantCode={}", tenantCode);

        if (tenantCode == null || tenantCode.trim().isEmpty()) {
            return Result.error(400, "租户编码不能为空");
        }

        // 查询租户
        TenantInfo tenant = tenantConfigService.getTenantByCode(tenantCode);

        if (tenant == null) {
            log.warn("【租户验证失败】租户不存在：tenantCode={}", tenantCode);
            return Result.error(404, "租户不存在");
        }

        if (tenant.getStatus() == 0) {
            log.warn("【租户验证失败】租户已禁用：tenantCode={}", tenantCode);
            return Result.error(403, "租户已被禁用");
        }

        // 构建返回数据
        Map<String, Object> result = new HashMap<>();
        result.put("tenantId", tenant.getId());

        log.info("【租户验证成功】tenantCode={}, tenantId={}", tenantCode, tenant.getId());
        return Result.success(result);
    }
}
