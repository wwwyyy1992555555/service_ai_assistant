package com.myproject.service_ai_assistant.controller;

import com.myproject.service_ai_assistant.common.Result;
import com.myproject.service_ai_assistant.dto.SendVerifyCodeRequest;
import com.myproject.service_ai_assistant.dto.TenantRegisterRequest;
import com.myproject.service_ai_assistant.service.TenantInfoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * 租户注册控制器（公开接口，无需登录）
 */
@Slf4j
@RestController
@RequestMapping("/api/tenant/register")
@Tag(name = "租户注册接口")
public class TenantRegisterController {

    @Autowired
    private TenantInfoService tenantInfoService;

    /**
     * 发送注册验证码
     */
    @PostMapping("/send-verify-code")
    @Operation(summary = "发送注册验证码", description = "向指定邮箱发送注册验证码")
    public Result<String> sendVerifyCode(@Valid @RequestBody SendVerifyCodeRequest request) {
        log.info("【发送验证码】开始：contactEmail={}", request.getContactEmail());

        try {
            tenantInfoService.sendVerifyCode(request.getContactEmail());
            log.info("【发送验证码】成功：contactEmail={}", request.getContactEmail());
            return Result.success("验证码已发送至您的邮箱");

        } catch (Exception e) {
            log.error("【发送验证码】失败：{}", e.getMessage(), e);
            throw e;
        }
    }

    /**
     * 租户自助注册
     */
    @PostMapping("/register")
    @Operation(summary = "租户自助注册", description = "企业用户自助注册，自动生成 tenant_code 和管理员账号")
    public Result<Map<String, String>> register(@Valid @RequestBody TenantRegisterRequest request) {
        log.info("【租户注册】开始：tenantName={}, contactPerson={}", 
                request.getTenantName(), request.getContactPerson());

        try {
            String tenantCode = tenantInfoService.registerTenant(
                    request.getTenantName(),
                    request.getContactPerson(),
                    request.getContactPhone(),
                    request.getContactEmail(),
                    request.getVerifyCode(),
                    request.getAdminUsername(),
                    request.getAdminPassword()
            );

            Map<String, String> result = new HashMap<>();
            result.put("tenantCode", tenantCode);
            result.put("message", "注册成功！请使用 tenant_code 配置 chat 页面");

            log.info("【租户注册】成功：tenantCode={}", tenantCode);
            return Result.success(result);

        } catch (Exception e) {
            log.error("【租户注册】失败：{}", e.getMessage(), e);
            throw e;
        }
    }
}
