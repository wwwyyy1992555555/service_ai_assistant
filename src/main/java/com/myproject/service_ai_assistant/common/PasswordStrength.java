package com.myproject.service_ai_assistant.common;

/**
 * 密码强度等级
 */
public enum PasswordStrength {

    /**
     * 非常弱
     */
    VERY_WEAK(0, "非常弱", "#ff4d4f"),

    /**
     * 弱
     */
    WEAK(1, "弱", "#ff7a45"),

    /**
     * 一般
     */
    MEDIUM(2, "一般", "#faad14"),

    /**
     * 强
     */
    STRONG(3, "强", "#52c41a"),

    /**
     * 非常强
     */
    VERY_STRONG(4, "非常强", "#1890ff");

    private final int level;
    private final String label;
    private final String color;

    PasswordStrength(int level, String label, String color) {
        this.level = level;
        this.label = label;
        this.color = color;
    }

    public int getLevel() {
        return level;
    }

    public String getLabel() {
        return label;
    }

    public String getColor() {
        return color;
    }

    /**
     * 根据分数获取密码强度等级
     * @param score 分数 (0-100)
     * @return 密码强度等级
     */
    public static PasswordStrength fromScore(int score) {
        if (score < 20) {
            return VERY_WEAK;
        } else if (score < 40) {
            return WEAK;
        } else if (score < 60) {
            return MEDIUM;
        } else if (score < 80) {
            return STRONG;
        } else {
            return VERY_STRONG;
        }
    }
}
