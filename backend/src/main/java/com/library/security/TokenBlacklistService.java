package com.library.security;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

/**
 * Token 黑名单(Redis): 登出/异常时将 jti 拉黑, 剩余有效期即失效
 */
@Service
@RequiredArgsConstructor
public class TokenBlacklistService {

    private static final String KEY_PREFIX = "jwt:blacklist:";

    private final StringRedisTemplate redisTemplate;

    public void blacklist(String jti, long ttlSeconds) {
        if (jti == null || jti.isEmpty() || ttlSeconds <= 0) {
            return;
        }
        redisTemplate.opsForValue().set(KEY_PREFIX + jti, "1", Duration.ofSeconds(ttlSeconds));
    }

    public boolean isBlacklisted(String jti) {
        if (jti == null || jti.isEmpty()) {
            return false;
        }
        return Boolean.TRUE.equals(redisTemplate.hasKey(KEY_PREFIX + jti));
    }
}
