package com.pat.crewhive.repository;

import com.pat.crewhive.model.auth.entity.RefreshToken;
import com.pat.crewhive.model.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, User> {

    void deleteByUser(User user);

    Optional<RefreshToken> findByToken(String token);

    @Query("""
        select rt from RefreshToken rt
        join fetch rt.user u
        left join fetch u.role r
        where rt.token = :token
    """)
    Optional<RefreshToken> findByTokenWithUserAndRole(String token);
}