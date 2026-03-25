package com.myproject.service_ai_assistant.controller;

import com.myproject.service_ai_assistant.common.Result;
import com.myproject.service_ai_assistant.dto.LoginRequest;
import com.myproject.service_ai_assistant.dto.UserDTO;
import com.myproject.service_ai_assistant.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;

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
}
