package com.myproject.service_ai_assistant.common;

import cn.hutool.crypto.digest.BCrypt;
import java.util.regex.Pattern;

/**
 * 密码安全工具类
 */
public class PasswordUtil {

    /**
     * 最小密码长度
     */
    private static final int MIN_PASSWORD_LENGTH = 6;

    /**
     * 强密码最小长度
     */
    private static final int STRONG_PASSWORD_LENGTH = 8;

    // 正则表达式
    private static final Pattern NUMBER_PATTERN = Pattern.compile("[0-9]");
    private static final Pattern LOWERCASE_PATTERN = Pattern.compile("[a-z]");
    private static final Pattern UPPERCASE_PATTERN = Pattern.compile("[A-Z]");
    private static final Pattern SPECIAL_CHAR_PATTERN = Pattern.compile("[^a-zA-Z0-9]");

    // 常见弱密码列表
    private static final String[] WEAK_PASSWORDS = {
        "123456", "password", "admin", "12345678", "qwerty",
        "123456789", "12345", "1234567", "letmein", "111111"
    };

    /**
     * 加密密码
     * @param rawPassword 原始密码
     * @return 加密后的密码
     */
    public static String encrypt(String rawPassword) {
        return BCrypt.hashpw(rawPassword, BCrypt.gensalt());
    }

    /**
     * 验证密码
     * @param rawPassword 原始密码
     * @param hashedPassword 加密后的密码
     * @return 是否匹配
     */
    public static boolean verify(String rawPassword, String hashedPassword) {
        try {
            return BCrypt.checkpw(rawPassword, hashedPassword);
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 校验密码强度（基础版）
     * @param password 密码
     * @throws IllegalArgumentException 密码不符合要求时抛出
     */
    public static void validatePasswordStrength(String password) {
        validateBasic(password);
    }

    /**
     * 校验密码强度（加强版）
     * @param password 密码
     * @throws IllegalArgumentException 密码不符合要求时抛出
     */
    public static void validateStrongPassword(String password) {
        validateBasic(password);
        
        // 检查长度（至少 8 位）
        if (password.length() < STRONG_PASSWORD_LENGTH) {
            throw new IllegalArgumentException("密码长度至少为 " + STRONG_PASSWORD_LENGTH + " 位");
        }

        // 检查是否包含数字
        if (!NUMBER_PATTERN.matcher(password).find()) {
            throw new IllegalArgumentException("密码必须包含数字");
        }

        // 检查是否包含字母
        if (!LOWERCASE_PATTERN.matcher(password).find() && 
            !UPPERCASE_PATTERN.matcher(password).find()) {
            throw new IllegalArgumentException("密码必须包含字母");
        }

        // 检查是否是弱密码
        if (isWeakPassword(password)) {
            throw new IllegalArgumentException("密码过于简单，请使用更复杂的密码");
        }
    }

    /**
     * 基础密码校验
     */
    private static void validateBasic(String password) {
        if (password == null || password.isEmpty()) {
            throw new IllegalArgumentException("密码不能为空");
        }

        if (password.length() < MIN_PASSWORD_LENGTH) {
            throw new IllegalArgumentException("密码长度至少为 " + MIN_PASSWORD_LENGTH + " 位");
        }
    }

    /**
     * 检查是否是弱密码
     */
    private static boolean isWeakPassword(String password) {
        String lowerPassword = password.toLowerCase();
        for (String weak : WEAK_PASSWORDS) {
            if (lowerPassword.equals(weak)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 计算密码强度分数
     * @param password 密码
     * @return 强度分数 (0-100)
     */
    public static int calculatePasswordStrength(String password) {
        int score = 0;

        // 长度评分（最多 30 分）
        if (password.length() >= 8) score += 20;
        if (password.length() >= 12) score += 10;

        // 字符类型评分（最多 40 分）
        if (NUMBER_PATTERN.matcher(password).find()) score += 10;
        if (LOWERCASE_PATTERN.matcher(password).find()) score += 10;
        if (UPPERCASE_PATTERN.matcher(password).find()) score += 10;
        if (SPECIAL_CHAR_PATTERN.matcher(password).find()) score += 10;

        // 复杂度评分（最多 30 分）
        int uniqueChars = (int) password.chars().distinct().count();
        if (uniqueChars >= 8) score += 15;
        if (uniqueChars >= 12) score += 15;

        return Math.min(score, 100);
    }
}
