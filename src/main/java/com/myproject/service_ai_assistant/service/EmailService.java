package com.myproject.service_ai_assistant.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

/**
 * 邮件服务 - 负责发送激活邮件、找回密码邮件等
 */
@Service
public class EmailService {

    @Autowired
    private JavaMailSender mailSender;

    @Autowired
    private org.springframework.core.env.Environment env;

    /**
     * 发送注册验证码邮件
     */
    public void sendVerifyCodeEmail(String toEmail, String verifyCode) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(env.getProperty("spring.mail.username")); // 设置发件人
        message.setTo(toEmail);
        message.setSubject("【AI智库平台】注册验证码");
        message.setText(buildVerifyCodeEmailContent(verifyCode));
        mailSender.send(message);
    }

    /**
     * 发送重置密码验证码邮件
     */
    public void sendResetPasswordEmail(String toEmail, String verifyCode) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(env.getProperty("spring.mail.username"));
        message.setTo(toEmail);
        message.setSubject("【AI智库平台】重置密码验证码");
        message.setText(buildResetPasswordEmailContent(verifyCode));
        mailSender.send(message);
    }

    /**
     * 构建验证码邮件内容
     */
    private String buildVerifyCodeEmailContent(String verifyCode) {
        return "尊敬的用户：\n\n"
                + "您正在注册 AI 智库企业咨询平台！\n\n"
                + "您的验证码为：" + verifyCode + "\n\n"
                + "验证码 5 分钟内有效，请勿泄露给他人。\n\n"
                + "如非本人操作，请忽略此邮件。\n\n"
                + "AI 智库平台团队";
    }

    /**
     * 构建重置密码邮件内容
     */
    private String buildResetPasswordEmailContent(String verifyCode) {
        return "尊敬的管理员：\n\n"
                + "您正在重置 AI 智库平台密码！\n\n"
                + "您的验证码为：" + verifyCode + "\n\n"
                + "验证码 5 分钟内有效，请勿泄露给他人。\n\n"
                + "如非本人操作，请立即联系平台管理员。\n\n"
                + "AI 智库平台团队";
    }
}
