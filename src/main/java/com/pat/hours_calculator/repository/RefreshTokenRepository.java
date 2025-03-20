package com.pat.hours_calculator.repository;

import com.pat.hours_calculator.model.auth.entities.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {
}