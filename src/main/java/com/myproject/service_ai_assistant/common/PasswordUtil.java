package com.myproject.service_ai_assistant.common;

import cn.hutool.crypto.digest.BCrypt;

/**
 * 密码工具类
 */
public class PasswordUtil {

    /**
     * 加密密码
     */
    public static String encode(String rawPassword) {
        return BCrypt.hashpw(rawPassword, BCrypt.gensalt());
    }

    /**
     * 验证密码
     */
    public static boolean matches(String rawPassword, String encodedPassword) {
        return BCrypt.checkpw(rawPassword, encodedPassword);
    }

    /**
     * 测试用：生成 BCrypt 密码
     */
    public static void main(String[] args) {
        String password = "123456";
        String encoded = encode(password);
        System.out.println("原始密码：" + password);
        System.out.println("加密后：" + encoded);
        System.out.println("验证：" + matches(password, encoded));
    }
}
