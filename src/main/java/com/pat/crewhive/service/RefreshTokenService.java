package com.pat.crewhive.service;

import com.pat.crewhive.security.exception.custom.ResourceNotFoundException;
import com.pat.crewhive.model.user.entity.User;
import com.pat.crewhive.model.auth.entity.RefreshToken;
import com.pat.crewhive.repository.RefreshTokenRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
public class RefreshTokenService {

    private final RefreshTokenRepository repo;

    @Autowired
    public RefreshTokenService(RefreshTokenRepository repo) {
        this.repo = repo;
    }

    /**
     * Generates a new refresh token for the given user.
     * Deletes any existing tokens for the user before creating a new one.
     *
     * @param user The user for whom the refresh token is generated.
     * @return The generated refresh token as a String.
     */
    public String generateRefreshToken(User user) {

        repo.deleteByUser(user);

        RefreshToken token = new RefreshToken();
        token.setUser(user);
        token.setToken(UUID.randomUUID().toString());
        token.setExpirationDate(LocalDate.now().plusDays(15));

        repo.save(token);

        log.info("Generated refresh token for user: {}", user.getUsername());
        log.info("Expiration date: {}", token.getExpirationDate());

        return token.getToken();
    }

    /**
     * Retrieves the refresh token for the given token string.
     *
     * @param token The refresh token string to retrieve.
     * @return The RefreshToken object if found.
     * @throws ResourceNotFoundException if the token is not found.
     */
    public RefreshToken getRefreshToken(String token) {

        Optional<RefreshToken> rt = repo.findByToken(token);

        if (rt.isEmpty()) {

            log.error("Refresh token not found for token: {}", token);
            throw new ResourceNotFoundException("Refresh token not found");
        }

        log.info("Found refresh token for user: {}", rt.get().getUser().getUsername());
        log.info("Expiration date: {}", rt.get().getExpirationDate());

        return rt.get();
    }

    /**
     * Checks if the given refresh token is expired.
     *
     * @param rt The refresh token to check.
     * @return true if the token is expired, false otherwise.
     */
    public boolean isExpired(RefreshToken rt) {

        if (rt == null) {

            log.error("Refresh token is null");
            throw new ResourceNotFoundException("Refresh token not found");
        }

        log.info("Checking expiration");
        log.info("Expiration date: {}", rt.getExpirationDate());
        log.info("Is expired: {}", rt.getExpirationDate().isBefore(LocalDate.now()));

        return rt.getExpirationDate().isBefore(LocalDate.now());
    }

    /**
     * Rotates the given refresh token by generating a new token and updating the expiration date.
     *
     * @param rt The refresh token to rotate.
     * @return The new refresh token as a String.
     */
    public String rotateRefreshToken(RefreshToken rt) {

        rt.setToken(UUID.randomUUID().toString());
        rt.setExpirationDate(LocalDate.now().plusDays(15));

        repo.save(rt);

        log.info("Rotated refresh token for user: {}", rt.getUser().getUsername());
        log.info("New expiration date: {}", rt.getExpirationDate());

        return rt.getToken();
    }

    /**
     * Retrieves the owner of the given refresh token.
     *
     * @param rt The refresh token to check.
     * @return The User who owns the refresh token.
     */
    public User getOwner(RefreshToken rt) {

        if (rt == null || rt.getUser() == null) {

            log.error("Refresh token or user is null");
            throw new ResourceNotFoundException("Refresh token or user not found");
        }

        log.info("Found owner for token: {}", rt.getUser().getUsername());

        return rt.getUser();
    }

    /**
     * Invalidates the given refresh token by deleting it from the repository.
     *
     * @param rt The refresh token to invalidate.
     */
    public void invalidateRefreshToken(RefreshToken rt) {

        if (rt == null) {

            log.error("Refresh token is null");
            throw new ResourceNotFoundException("Refresh token not found");
        }

        repo.delete(rt);

        log.info("Invalidated refresh token for user: {}", rt.getUser().getUsername());
    }
}
