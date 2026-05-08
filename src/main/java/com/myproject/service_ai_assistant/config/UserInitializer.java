package com.myproject.service_ai_assistant.config;

import cn.hutool.crypto.digest.BCrypt;
import com.myproject.service_ai_assistant.common.LevelCode;
import com.myproject.service_ai_assistant.entity.User;
import com.myproject.service_ai_assistant.mapper.UserMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

/**
 * 用户初始化配置
 */
@Slf4j
@Component
public class UserInitializer implements CommandLineRunner {

    @Autowired
    private UserMapper userMapper;
    
    @Autowired
    private SuperAdminConfig superAdminConfig;

    @Override
    public void run(String... args) {
        try {
            log.info("【用户初始化】开始检查默认用户...");

            // 检查超级管理员用户是否存在（tenant_id=0）
            String adminUsername = superAdminConfig.getUsername();
            User adminUser = userMapper.selectByUsername(LevelCode.ROLE_LEVEL_TENANT_ID, adminUsername);
            if (adminUser == null) {
                log.info("【用户初始化】创建默认超级管理员用户：username={}", adminUsername);
                createSuperAdmin(
                    adminUsername,
                    superAdminConfig.getRealName(),
                    superAdminConfig.getPassword()
                );
            } else {
                log.info("【用户初始化】超级管理员用户已存在：username={}", adminUsername);
            }

            log.info("【用户初始化】完成");
        } catch (Exception e) {
            log.error("【用户初始化】失败：{}", e.getMessage(), e);
            log.error("【用户初始化】请确保已手动执行 init.sql 脚本创建数据库表结构");
            throw new RuntimeException("用户初始化失败，请检查数据库表是否存在", e);
        }
    }

    /**
     * 创建超级管理员用户
     */
    private void createSuperAdmin(String username, String realName, String password) {
        User user = new User();
        user.setTenantId(LevelCode.ROLE_LEVEL_TENANT_ID); // tenant_id = 0
        user.setUsername(username);
        user.setPassword(BCrypt.hashpw(password, BCrypt.gensalt()));
        user.setRealName(realName);
        user.setPhone("13900139000");
        user.setEmail(username + "@platform.com");
        user.setRoleLevel(LevelCode.ROLE_LEVEL_SUPER_ADMIN); // role_level = 0
        user.setStatus(1);
        userMapper.insert(user);
        log.info("【用户初始化】超级管理员创建成功：username={}, realName={}", username, realName);
    }
}