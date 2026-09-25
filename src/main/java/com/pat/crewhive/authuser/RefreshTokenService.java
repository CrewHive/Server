package com.pat.crewhive.authuser;

import com.pat.crewhive.security.exception.custom.InvalidTokenException;
import com.pat.crewhive.user.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.HexFormat;
import java.util.UUID;

@Service
public class RefreshTokenService {

    private static final Logger log = LoggerFactory.getLogger(RefreshTokenService.class);

    static final Duration REFRESH_TOKEN_TTL = Duration.ofDays(15);

    /**
     * Outcome of a successful rotation.
     *
     * @param user         the owner of the rotated token
     * @param refreshToken the new raw refresh token (the only place it ever exists in clear)
     */
    public record Rotation(User user, String refreshToken) {
    }

    private final RefreshTokenRepository repo;
    private final Clock clock;

    public RefreshTokenService(RefreshTokenRepository repo, Clock clock) {
        this.repo = repo;
        this.clock = clock;
    }

    /**
     * Starts a new session for the user: every existing token of the user is deleted and a token
     * belonging to a brand-new family is issued.
     *
     * @param user The user for whom the refresh token is generated.
     * @return The raw refresh token. Only its hash is persisted.
     */
    @Transactional
    public String issueNewFamily(User user) {

        repo.deleteByUser(user);

        String raw = save(user, UUID.randomUUID());

        log.info("issueNewFamily: Issued new refresh token family for userId={}", user.getUserId());

        return raw;
    }

    /**
     * Rotates a refresh token: the presented token is marked as used and a new one of the same
     * family is issued.
     * <p>
     * Presenting a token that was already rotated means it has been copied: the whole family is
     * revoked, so that both the thief and the legitimate owner have to authenticate again.
     * {@code noRollbackFor} is required, otherwise the revocation would be rolled back together
     * with the exception thrown to reject the request.
     *
     * @param rawToken The raw refresh token presented by the client.
     * @return The owner of the token and the new raw refresh token.
     * @throws InvalidTokenException if the token is unknown, expired or was already used.
     */
    @Transactional(noRollbackFor = InvalidTokenException.class)
    public Rotation rotate(String rawToken) {

        Instant now = clock.instant();

        RefreshToken rt = repo.findByTokenHashWithUserAndRole(hash(rawToken))
                .orElseThrow(() -> new InvalidTokenException("Invalid refresh token"));

        // markUsed is the atomic gate: of two concurrent presentations of the same token
        // only one flips usedAt, the other one is treated exactly like a replay.
        if (rt.getUsedAt() != null || repo.markUsed(rt.getRefreshTokenId(), now) == 0) {

            repo.deleteByFamilyId(rt.getFamilyId());

            log.warn("rotate: Refresh token reuse detected, family revoked: userId={}, familyId={}",
                    rt.getUser().getUserId(), rt.getFamilyId());

            throw new InvalidTokenException("Invalid refresh token");
        }

        if (!rt.getExpiresAt().isAfter(now)) {

            log.info("rotate: Expired refresh token for userId={}", rt.getUser().getUserId());

            throw new InvalidTokenException("Invalid refresh token");
        }

        User user = rt.getUser();

        String newRaw = save(user, rt.getFamilyId());

        // Used tokens are kept until they expire (that is what makes reuse detectable):
        // this is the only place where they get purged.
        repo.deleteExpiredByUser(user, now);

        log.info("rotate: Rotated refresh token for userId={}", user.getUserId());

        return new Rotation(user, newRaw);
    }

    /**
     * Finds a live (not expired) token by its raw value, for logout.
     *
     * @param rawToken The raw refresh token.
     * @return The token, with user and roles loaded.
     * @throws InvalidTokenException if the token is unknown or expired.
     */
    @Transactional(readOnly = true)
    public RefreshToken getValidToken(String rawToken) {

        RefreshToken rt = repo.findByTokenHashWithUserAndRole(hash(rawToken))
                .orElseThrow(() -> new InvalidTokenException("Refresh Token expired or missing"));

        if (!rt.getExpiresAt().isAfter(clock.instant())) {

            throw new InvalidTokenException("Refresh Token expired or missing");
        }

        return rt;
    }

    /**
     * Revokes every token of the family the given token belongs to.
     *
     * @param rt A token of the family to revoke.
     */
    @Transactional
    public void revokeFamily(RefreshToken rt) {

        repo.deleteByFamilyId(rt.getFamilyId());

        log.info("revokeFamily: Revoked refresh token family for userId={}", rt.getUser().getUserId());
    }

    /**
     * Deletes every refresh token associated with the given user.
     *
     * @param user The user whose refresh tokens are to be deleted.
     */
    @Transactional
    public void deleteTokenByUser(User user) {

        repo.deleteByUser(user);

        log.info("deleteTokenByUser: Deleted refresh tokens for userId={}", user.getUserId());
    }

    private String save(User user, UUID familyId) {

        String raw = UUID.randomUUID().toString();

        RefreshToken token = new RefreshToken();
        token.setUser(user);
        token.setFamilyId(familyId);
        token.setTokenHash(hash(raw));
        token.setExpiresAt(clock.instant().plus(REFRESH_TOKEN_TTL));

        repo.save(token);

        return raw;
    }

    /**
     * SHA-256 hex of the raw token. A fast hash is enough here: the token is a random 122-bit
     * value, not a low-entropy secret, so the only goal is that a leaked table is not spendable.
     */
    static String hash(String rawToken) {

        try {
            return HexFormat.of().formatHex(
                    MessageDigest.getInstance("SHA-256").digest(rawToken.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }
}
