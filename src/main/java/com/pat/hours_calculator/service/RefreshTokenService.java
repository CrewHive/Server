package com.pat.hours_calculator.service;

import com.pat.hours_calculator.dto.UserDTO;
import com.pat.hours_calculator.model.user.entities.User;
import com.pat.hours_calculator.model.auth.entities.RefreshToken;
import com.pat.hours_calculator.repository.RefreshTokenRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.UUID;

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

        return token.getToken();
    }

    public boolean isExpired(String token){

        RefreshToken rt = repo.findByToken(token).orElseThrow(() -> new EntityNotFoundException("Token not found"));

        return rt.getExpirationDate().isBefore(LocalDate.now());
    }

    public String rotateRefreshToken(String token) throws Exception {

        RefreshToken rt = repo.findByToken(token).orElseThrow(() -> new EntityNotFoundException("Token not found"));

        rt.setToken(UUID.randomUUID().toString());
        rt.setExpirationDate(LocalDate.now().plusDays(15));

        repo.save(rt);

        return rt.getToken();
    }
}
