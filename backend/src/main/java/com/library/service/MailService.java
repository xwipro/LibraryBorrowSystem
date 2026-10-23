package com.library.service;

import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

/**
 * 邮件服务: 找回密码验证码
 * 注意: 当前 SMTP 为占位默认配置, 后续替换为正式邮件接口后仅需调整此处实现(异步调用, 不等待返回)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MailService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username:placeholder@example.com}")
    private String from;

    /**
     * 异步发送验证码邮件, 不阻塞业务调用方
     */
    @Async
    public void sendVerifyCode(String email, String code) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(from);
            helper.setTo(email);
            helper.setSubject("图书管理系统 - 找回密码验证码");
            helper.setText("您的找回密码验证码为：<b>" + code + "</b>，5 分钟内有效。"
                    + "如非本人操作请忽略本邮件。", true);
            mailSender.send(message);
            log.info("验证码邮件已发送至 {}", email);
        } catch (Exception e) {
            // 邮件发送失败不阻断业务(后续接入正式 SMTP 后此处应有重试/告警)
            log.error("验证码邮件发送失败 to={}", email, e);
        }
    }

    /**
     * 异步发送密码重置通知邮件(含新密码, 提示尽快修改), 不阻塞业务调用方
     */
    @Async
    public void sendResetPassword(String email, String username, String newPassword) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(from);
            helper.setTo(email);
            helper.setSubject("图书管理系统 - 密码已重置");
            helper.setText(
                    "尊敬的 " + username + "：<br/><br/>"
                            + "您的账号密码已被管理员重置，新密码为：<b>" + newPassword + "</b><br/><br/>"
                            + "为保障账号安全，请<b>尽快登录系统并修改密码</b>，防止密码被他人利用。<br/>"
                            + "如非本人操作，请立即联系系统管理员。",
                    true);
            mailSender.send(message);
            log.info("密码重置邮件已发送至 {}", email);
        } catch (Exception e) {
            // 邮件发送失败不阻断业务(后续接入正式 SMTP 后此处应有重试/告警)
            log.error("密码重置邮件发送失败 to={}", email, e);
        }
    }
}
