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

    /**
     * Takes the jti value and the time left to live of the token
     * and puts it into the Redis Cache where it will be put
     * into the black list till it expires
     * @param jti The UUID of the JWT
     * @param expiration The expiration of the Refresh Token
     */
    public void revoke(String jti, Date expiration) {

        Duration ttl  = Duration.between(Instant.now(), expiration.toInstant());

        if (ttl.toMillis() <= 0) return;

        redisTemplate.opsForValue().set("revoked-jti:"+jti, "1", ttl);
    }

    /**
     * Check if the user has a refresh token expired
     * @param jti The UUID of the JWT
     * @return True if the user has a revoked token, otherwise False
     */
    public Boolean isRevoked(String jti) {
        return redisTemplate.hasKey("revoked-jti:"+jti);
    }
}
