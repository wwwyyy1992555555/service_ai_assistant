package com.myproject.service_ai_assistant.controller;

import com.myproject.service_ai_assistant.common.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.concurrent.TimeUnit;

/**
 * Redis 测试控制器
 */
@RestController
@RequestMapping("/test/redis")
@Tag(name = "Redis 测试接口")
public class RedisTestController {

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @GetMapping("/set")
    @Operation(summary = "设置 Redis 值")
    public Result<Boolean> set(@RequestParam String key, @RequestParam String value) {
        redisTemplate.opsForValue().set(key, value, 30, TimeUnit.MINUTES);
        return Result.success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "获取 Redis 值")
    public Result<String> get(@RequestParam String key) {
        Object value = redisTemplate.opsForValue().get(key);
        return Result.success(value != null ? value.toString() : null);
    }

    @GetMapping("/ping")
    @Operation(summary = "测试 Redis 连接")
    public Result<String> ping() {
        try {
            redisTemplate.opsForValue().set("health_check", "ok", 10, TimeUnit.SECONDS);
            String value = (String) redisTemplate.opsForValue().get("health_check");
            if ("ok".equals(value)) {
                return Result.success("Redis 连接正常！");
            } else {
                return Result.error("Redis 响应异常");
            }
        } catch (Exception e) {
            return Result.error("Redis 连接失败：" + e.getMessage());
        }
    }
}
