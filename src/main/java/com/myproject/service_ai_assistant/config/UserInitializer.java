package com.myproject.service_ai_assistant.config;

import cn.hutool.crypto.digest.BCrypt;
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

    @Override
    public void run(String... args) {
        try {
            log.info("【用户初始化】开始检查默认用户...");

            // 检查 admin 用户是否存在
            User adminUser = userMapper.selectByUsername(1L, "admin");
            if (adminUser == null) {
                log.info("【用户初始化】创建默认管理员用户...");
                createUser("admin", "管理员", "123456");
            } else {
                log.info("【用户初始化】admin 用户已存在");
            }

            // 检查 operator 用户是否存在
            User operatorUser = userMapper.selectByUsername(1L, "operator");
            if (operatorUser == null) {
                log.info("【用户初始化】创建默认操作员用户...");
                createUser("operator", "操作员", "123456");
            } else {
                log.info("【用户初始化】operator 用户已存在");
            }

            log.info("【用户初始化】完成");
        } catch (Exception e) {
            log.error("【用户初始化】失败：{}", e.getMessage(), e);
            log.error("【用户初始化】请确保已手动执行 init.sql 脚本创建数据库表结构");
            throw new RuntimeException("用户初始化失败，请检查数据库表是否存在", e);
        }
    }

    /**
     * 创建用户
     */
    private void createUser(String username, String realName, String password) {
        User user = new User();
        user.setTenantId(1L);
        user.setUsername(username);
        user.setPassword(BCrypt.hashpw(password, BCrypt.gensalt()));
        user.setRealName(realName);
        user.setPhone("13800138000");
        user.setEmail(username + "@gov.cn");
        user.setRole("admin");
        user.setStatus(1);
        userMapper.insert(user);
        log.info("【用户初始化】用户创建成功：username={}", username);
    }
}
