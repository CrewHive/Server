package com.pat.crewhive.security;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.Date;

@Service
public class TokenBlackListService {

    private final StringRedisTemplate redisTemplate;

    TokenBlackListService(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public void revoke(String jti, Date expiration) {

        Duration ttl  = Duration.between(Instant.now(), expiration.toInstant());

        if (ttl.toMillis() <= 0) return;

        redisTemplate.opsForValue().set("revoked-jti:"+jti, "1", ttl);
    }

    public Boolean isRevoked(String jti) {
        return redisTemplate.hasKey("revoked-jti:"+jti);
    }
}
