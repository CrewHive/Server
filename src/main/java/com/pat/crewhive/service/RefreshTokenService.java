package com.pat.crewhive.service;

import com.pat.crewhive.exception.custom.ResourceNotFoundException;
import com.pat.crewhive.model.user.entity.User;
import com.pat.crewhive.model.auth.entity.RefreshToken;
import com.pat.crewhive.repository.RefreshTokenRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.UUID;

@Slf4j
@Service
public class RefreshTokenService {

    private final RefreshTokenRepository repo;

    @Autowired
    public RefreshTokenService(RefreshTokenRepository repo) {
        this.repo = repo;
    }

    public String generateRefreshToken(User user) {

        repo.deleteByUser(user);

        RefreshToken token = new RefreshToken();
        token.setUser(user);
        token.setToken(UUID.randomUUID().toString());
        token.setExpirationDate(LocalDate.now().plusDays(15));

        repo.save(token);

        log.info("Generated refresh token for user: {}", user.getUsername());
        log.info("Token: {}", token.getToken());
        log.info("Expiration date: {}", token.getExpirationDate());

        return token.getToken();
    }

    public boolean isExpired(String token) {

        RefreshToken rt = repo.findByToken(token).orElseThrow(() -> new ResourceNotFoundException("Token not found"));

        log.info("Checking expiration for token: {}", token);
        log.info("Expiration date: {}", rt.getExpirationDate());
        log.info("Is expired: {}", rt.getExpirationDate().isBefore(LocalDate.now()));

        return rt.getExpirationDate().isBefore(LocalDate.now());
    }

    public String rotateRefreshToken(String token) {

        RefreshToken rt = repo.findByToken(token).orElseThrow(() -> new ResourceNotFoundException("Token not found"));

        rt.setToken(UUID.randomUUID().toString());
        rt.setExpirationDate(LocalDate.now().plusDays(15));

        repo.save(rt);

        log.info("Rotated refresh token for user: {}", rt.getUser().getUsername());
        log.info("New token: {}", rt.getToken());
        log.info("New expiration date: {}", rt.getExpirationDate());
        log.info("Old token: {}", token);

        return rt.getToken();
    }
}
