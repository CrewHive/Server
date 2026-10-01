package com.pat.crewhive.security;

import com.pat.crewhive.support.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.time.Instant;
import java.util.Date;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests (real Redis) for the per-user access token revocation (M5).
 */
class TokenBlackListServiceIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private TokenBlackListService tokenBlackListService;
    @Autowired
    private StringRedisTemplate redisTemplate;

    @Test
    void isRevokedForUser_isFalse_whenTheUserWasNeverRevoked() {
        assertThat(tokenBlackListService.isRevokedForUser(UUID.randomUUID(), new Date())).isFalse();
    }

    @Test
    void isRevokedForUser_rejectsTokensIssuedBeforeTheRevocation() {
        UUID userId = UUID.randomUUID();

        tokenBlackListService.revokeAllForUser(userId);

        Date issuedTwoSecondsAgo = Date.from(Instant.now().minusSeconds(2));
        assertThat(tokenBlackListService.isRevokedForUser(userId, issuedTwoSecondsAgo)).isTrue();
    }

    @Test
    void isRevokedForUser_acceptsTokensIssuedInTheSameSecondOrAfterTheRevocation() {
        UUID userId = UUID.randomUUID();

        tokenBlackListService.revokeAllForUser(userId);

        // the token issued right after a revocation (e.g. by leaveCompany) must survive
        assertThat(tokenBlackListService.isRevokedForUser(userId, new Date())).isFalse();
        assertThat(tokenBlackListService.isRevokedForUser(userId, Date.from(Instant.now().plusSeconds(5)))).isFalse();
    }

    @Test
    void isRevokedForUser_doesNotAffectOtherUsers() {
        UUID revoked = UUID.randomUUID();
        UUID other = UUID.randomUUID();

        tokenBlackListService.revokeAllForUser(revoked);

        Date old = Date.from(Instant.now().minusSeconds(2));
        assertThat(tokenBlackListService.isRevokedForUser(other, old)).isFalse();
    }

    @Test
    void revokeAllForUser_expiresAfterTheAccessTokenLifetime() {
        UUID userId = UUID.randomUUID();

        tokenBlackListService.revokeAllForUser(userId);

        Long ttlSeconds = redisTemplate.getExpire("revoked-user:" + userId, TimeUnit.SECONDS);
        assertThat(ttlSeconds).isPositive().isLessThanOrEqualTo(JwtService.ACCESS_TOKEN_TTL_MILLIS / 1000);
    }

    // ---- blacklist per jti ----

    @Test
    void revoke_makesTheJtiRevokedUntilItsExpiration() {
        String jti = UUID.randomUUID().toString();

        assertThat(tokenBlackListService.isRevoked(jti)).isFalse();

        tokenBlackListService.revoke(jti, Date.from(Instant.now().plusSeconds(60)));

        assertThat(tokenBlackListService.isRevoked(jti)).isTrue();
        Long ttl = redisTemplate.getExpire("revoked-jti:" + jti, TimeUnit.SECONDS);
        assertThat(ttl).isPositive().isLessThanOrEqualTo(60);
    }

    @Test
    void revoke_ofAnAlreadyExpiredToken_storesNothing() {
        String jti = UUID.randomUUID().toString();

        tokenBlackListService.revoke(jti, Date.from(Instant.now().minusSeconds(5)));

        assertThat(tokenBlackListService.isRevoked(jti)).isFalse();
    }

    @Test
    void revoke_doesNotAffectOtherJtis() {
        String revoked = UUID.randomUUID().toString();
        String other = UUID.randomUUID().toString();

        tokenBlackListService.revoke(revoked, Date.from(Instant.now().plusSeconds(60)));

        assertThat(tokenBlackListService.isRevoked(other)).isFalse();
    }
}
