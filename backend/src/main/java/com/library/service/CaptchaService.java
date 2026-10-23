package com.library.service;

import com.library.dto.auth.CaptchaResult;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.util.Base64;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 验证码服务: 生成 4 位数字验证码图片(base64), 校验码存入 Redis(带过期时间)
 */
@Service
@RequiredArgsConstructor
public class CaptchaService {

    private static final String KEY_PREFIX = "captcha:";
    private static final int WIDTH = 120;
    private static final int HEIGHT = 40;

    @Value("${app.captcha-expire-seconds:300}")
    private long expireSeconds;

    private final StringRedisTemplate redisTemplate;

    public CaptchaResult generate() {
        String code = String.format("%04d", ThreadLocalRandom.current().nextInt(10000));
        String key = UUID.randomUUID().toString().replace("-", "");

        String img = drawImage(code);
        redisTemplate.opsForValue().set(KEY_PREFIX + key, code,
                java.time.Duration.ofSeconds(expireSeconds));
        return new CaptchaResult(key, img);
    }

    /** 校验验证码, 无论成败均删除(一次性) */
    public boolean verify(String key, String inputCode) {
        if (key == null || inputCode == null) {
            return false;
        }
        String cached = redisTemplate.opsForValue().get(KEY_PREFIX + key);
        redisTemplate.delete(KEY_PREFIX + key);
        return cached != null && cached.equalsIgnoreCase(inputCode.trim());
    }

    private String drawImage(String code) {
        BufferedImage image = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        // 背景
        g.setColor(new Color(243, 247, 252));
        g.fillRect(0, 0, WIDTH, HEIGHT);
        // 干扰线
        g.setColor(new Color(190, 205, 225));
        for (int i = 0; i < 5; i++) {
            g.drawLine(ThreadLocalRandom.current().nextInt(WIDTH), ThreadLocalRandom.current().nextInt(HEIGHT),
                    ThreadLocalRandom.current().nextInt(WIDTH), ThreadLocalRandom.current().nextInt(HEIGHT));
        }
        // 数字
        g.setFont(new Font("Arial", Font.BOLD, 24));
        for (int i = 0; i < code.length(); i++) {
            g.setColor(new Color(37, 99, 235));
            g.drawString(String.valueOf(code.charAt(i)), 14 + i * 24, 29);
        }
        g.dispose();

        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            ImageIO.write(image, "png", out);
            return "data:image/png;base64," + Base64.getEncoder().encodeToString(out.toByteArray());
        } catch (Exception e) {
            throw new IllegalStateException("验证码生成失败", e);
        }
    }
}
