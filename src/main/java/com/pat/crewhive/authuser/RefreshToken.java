package com.pat.crewhive.authuser;


import com.pat.crewhive.user.User;
import jakarta.persistence.*;

import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "refresh_token", indexes = {
        @Index(name = "idx_refreshtoken", columnList = "refresh_token_id"),
        @Index(name = "idx_refreshtoken_user_id", columnList = "user_id")
})
public class RefreshToken {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "refresh_token_id", nullable = false)
    private UUID refreshTokenId;

    @Column(name = "token", nullable = false)
    private String token;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Column(name="expiration_date", nullable = false)
    private LocalDate expirationDate;

    public RefreshToken() {
    }

    public RefreshToken(UUID refreshTokenId, String token, User user, LocalDate expirationDate) {
        this.refreshTokenId = refreshTokenId;
        this.token = token;
        this.user = user;
        this.expirationDate = expirationDate;
    }

    public UUID getRefreshTokenId() {
        return refreshTokenId;
    }

    public void setRefreshTokenId(UUID refreshTokenId) {
        this.refreshTokenId = refreshTokenId;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public LocalDate getExpirationDate() {
        return expirationDate;
    }

    public void setExpirationDate(LocalDate expirationDate) {
        this.expirationDate = expirationDate;
    }

}
