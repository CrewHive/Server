package com.pat.crewhive.authuser;

import com.pat.crewhive.user.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    void deleteByUser(User user);

    Optional<RefreshToken> findByToken(String token);

    Optional<RefreshToken> findByUser(User user);

    @Query("""
        select rt from RefreshToken rt
        join fetch rt.user u
        left join fetch u.role r
        where rt.token = :token
    """)
    Optional<RefreshToken> findByTokenWithUserAndRole(String token);
}