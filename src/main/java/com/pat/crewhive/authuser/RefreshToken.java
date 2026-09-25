package com.pat.crewhive.authuser;


import com.pat.crewhive.user.User;
import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

/**
 * A refresh token, stored only as SHA-256 hash: the raw value is known to the client alone.
 * <p>
 * Tokens issued by successive rotations of the same session share a {@code familyId}. A rotated
 * token is kept (with {@code usedAt} set) until it expires, so that its reuse can be detected.
 */
@Entity
@Table(name = "refresh_token", indexes = {
        @Index(name = "idx_refreshtoken_family_id", columnList = "family_id"),
        @Index(name = "idx_refreshtoken_user_id", columnList = "user_id")
})
public class RefreshToken {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "refresh_token_id", nullable = false)
    private UUID refreshTokenId;

    @Column(name = "token_hash", nullable = false, unique = true, length = 64)
    private String tokenHash;

    @Column(name = "family_id", nullable = false)
    private UUID familyId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "used_at")
    private Instant usedAt;

    public RefreshToken() {
    }

    public RefreshToken(UUID refreshTokenId, String tokenHash, UUID familyId, User user, Instant expiresAt, Instant usedAt) {
        this.refreshTokenId = refreshTokenId;
        this.tokenHash = tokenHash;
        this.familyId = familyId;
        this.user = user;
        this.expiresAt = expiresAt;
        this.usedAt = usedAt;
    }

    public UUID getRefreshTokenId() {
        return refreshTokenId;
    }

    public void setRefreshTokenId(UUID refreshTokenId) {
        this.refreshTokenId = refreshTokenId;
    }

    public String getTokenHash() {
        return tokenHash;
    }

    public void setTokenHash(String tokenHash) {
        this.tokenHash = tokenHash;
    }

    public UUID getFamilyId() {
        return familyId;
    }

    public void setFamilyId(UUID familyId) {
        this.familyId = familyId;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(Instant expiresAt) {
        this.expiresAt = expiresAt;
    }

    public Instant getUsedAt() {
        return usedAt;
    }

    public void setUsedAt(Instant usedAt) {
        this.usedAt = usedAt;
    }

}
