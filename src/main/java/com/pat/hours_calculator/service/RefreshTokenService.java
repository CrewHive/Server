package com.pat.hours_calculator.service;

import com.pat.hours_calculator.model.user.entities.User;
import com.pat.hours_calculator.model.auth.entities.RefreshToken;
import com.pat.hours_calculator.repository.RefreshTokenRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

@Service
public class RefreshTokenService {

    private RefreshTokenRepository repo;

    @Autowired
    public RefreshTokenService(RefreshTokenRepository repo) {
        this.repo = repo;
    }

    public String createRefreshToken(User user) {

        repo.deleteByUser(user);

        RefreshToken token = new RefreshToken();
        token.setUser(user);
        token.setToken(UUID.randomUUID().toString());
        repo.save(token);

        return token.getToken();
    }

    public boolean isExpired(String token) throws Exception{

        RefreshToken rt = repo.findByToken(token).orElseThrow();

        return rt.getExpirationDate().isBefore(LocalDate.now());
    }
}
