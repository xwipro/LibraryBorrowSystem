package com.library.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.UUID;

/**
 * JWT 工具: 签发 / 解析 / 校验
 * 规则: token 默认 12 小时过期; 携带 userId/username/role/jti
 */
@Component
public class JwtUtil {

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.expire-hours}")
    private int expireHours;

    private SecretKey key() {
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    public String generateToken(Long userId, String username, String roleCode) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + expireHours * 3600_000L);
        return Jwts.builder()
                .id(UUID.randomUUID().toString())
                .subject(username)
                .claim("userId", userId)
                .claim("role", roleCode == null ? "" : roleCode)
                .issuedAt(now)
                .expiration(expiry)
                .signWith(key())
                .compact();
    }

    /** 解析 token, 非法/过期抛异常 */
    public Claims parse(String token) {
        return Jwts.parser().verifyWith(key()).build()
                .parseSignedClaims(token).getPayload();
    }

    /** 校验是否有效(不抛异常) */
    public boolean validate(String token) {
        try {
            parse(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public String getJti(Claims claims) {
        return claims.getId();
    }
}
