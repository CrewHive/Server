package com.pat.hours_calculator.repository;

import com.pat.hours_calculator.model.auth.entities.RefreshToken;
import com.pat.hours_calculator.model.user.entities.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.Optional;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, User> {
    void deleteByUser(User user);
    Optional<RefreshToken> findByToken(String token);
}