package com.pat.crewhive.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;

@Service
public class TokenBlackListService {

    private static final Logger log = LoggerFactory.getLogger(TokenBlackListService.class);

    private static final String JTI_PREFIX = "revoked-jti:";
    private static final String USER_PREFIX = "revoked-user:";

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

        redisTemplate.opsForValue().set(JTI_PREFIX + jti, "1", ttl);
    }

    /**
     * Check if the user has a refresh token expired
     * @param jti The UUID of the JWT
     * @return True if the user has a revoked token, otherwise False
     */
    public Boolean isRevoked(String jti) {
        return redisTemplate.hasKey(JTI_PREFIX + jti);
    }

    /**
     * Invalidates every access token already issued to the user (deactivation, company removal,
     * role or password change, logout). Stores the revocation instant in epoch seconds, the same
     * precision as the JWT {@code iat} claim, for as long as an access token can live: after
     * that every older token has expired anyway.
     * <p>
     * Tokens issued in the same second as the revocation are NOT rejected (see
     * {@link #isRevokedForUser}): this lets the token issued right after the revocation (e.g. in
     * {@code leaveCompany}) through, at the cost that an old token issued within that same second
     * survives until it expires.
     *
     * @param userId The ID of the user whose access tokens are revoked
     */
    public void revokeAllForUser(UUID userId) {

        long cutoffSeconds = Instant.now().getEpochSecond();

        redisTemplate.opsForValue().set(USER_PREFIX + userId, String.valueOf(cutoffSeconds),
                Duration.ofMillis(JwtService.ACCESS_TOKEN_TTL_MILLIS));

        log.info("Revoked all access tokens for userId={}", userId);
    }

    /**
     * Checks whether the access token was issued before the user's last revocation.
     *
     * @param userId   The ID of the user the token belongs to
     * @param issuedAt The {@code iat} of the token
     * @return True if the token predates the revocation of the user's tokens, otherwise False
     */
    public boolean isRevokedForUser(UUID userId, Date issuedAt) {

        String cutoff = redisTemplate.opsForValue().get(USER_PREFIX + userId);

        if (cutoff == null) return false;

        return issuedAt.toInstant().getEpochSecond() < Long.parseLong(cutoff);
    }
}
