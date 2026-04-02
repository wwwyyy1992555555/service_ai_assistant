package com.myproject.service_ai_assistant.controller;

import com.myproject.service_ai_assistant.common.PasswordStrength;
import com.myproject.service_ai_assistant.common.PasswordUtil;
import com.myproject.service_ai_assistant.dto.PasswordStrengthDTO;
import com.myproject.service_ai_assistant.dto.UserDTO;
import com.myproject.service_ai_assistant.exception.BusinessException;
import com.myproject.service_ai_assistant.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * 密码安全控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/password")
@Tag(name = "密码管理")
public class PasswordController {

    @Autowired
    private UserService userService;

    /**
     * 检测密码强度
     */
    @GetMapping("/check-strength")
    @Operation(summary = "检测密码强度")
    public PasswordStrengthDTO checkPasswordStrength(
            @Parameter(description = "密码", required = true) @RequestParam String password
    ) {
        // 计算分数
        int score = PasswordUtil.calculatePasswordStrength(password);
        PasswordStrength strength = PasswordStrength.fromScore(score);

        // 构建返回结果
        PasswordStrengthDTO dto = new PasswordStrengthDTO();
        dto.setScore(score);
        dto.setLevel(strength.name());
        dto.setLabel(strength.getLabel());
        dto.setColor(strength.getColor());
        dto.setMessage(getStrengthMessage(strength));

        log.info("【检测密码强度】score={}, level={}", score, strength.getLabel());
        return dto;
    }

    /**
     * 修改密码
     */
    @PostMapping("/change")
    @Operation(summary = "修改密码")
    public boolean changePassword(
            @Parameter(description = "用户 ID", required = true) @RequestParam Long userId,
            @Parameter(description = "旧密码", required = true) @RequestParam String oldPassword,
            @Parameter(description = "新密码", required = true) @RequestParam String newPassword
    ) {
        log.info("【修改密码】userId={}", userId);
        userService.changePassword(userId, oldPassword, newPassword);
        return true;
    }

    /**
     * 获取强度提示信息
     */
    private String getStrengthMessage(PasswordStrength strength) {
        switch (strength) {
            case VERY_WEAK:
                return "密码过于简单，建议至少 8 位并包含数字和字母";
            case WEAK:
                return "密码强度较弱，建议增加长度和复杂度";
            case MEDIUM:
                return "密码强度一般，建议添加特殊字符";
            case STRONG:
                return "密码强度良好";
            case VERY_STRONG:
                return "密码非常安全";
            default:
                return "";
        }
    }
}
