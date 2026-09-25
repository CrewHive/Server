package com.pat.crewhive.authuser;

import com.pat.crewhive.user.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, UUID> {

    @Modifying(flushAutomatically = true)
    @Query("delete from RefreshToken rt where rt.user = :user")
    void deleteByUser(@Param("user") User user);

    @Modifying(flushAutomatically = true)
    @Query("delete from RefreshToken rt where rt.familyId = :familyId")
    void deleteByFamilyId(@Param("familyId") UUID familyId);

    @Modifying(flushAutomatically = true)
    @Query("delete from RefreshToken rt where rt.user = :user and rt.expiresAt < :instant")
    void deleteExpiredByUser(@Param("user") User user, @Param("instant") Instant instant);

    @Query("""
        select rt from RefreshToken rt
        join fetch rt.user u
        left join fetch u.roles r
        left join fetch r.role
        where rt.tokenHash = :tokenHash
    """)
    Optional<RefreshToken> findByTokenHashWithUserAndRole(@Param("tokenHash") String tokenHash);

    /**
     * Atomically marks a token as used. Returns 1 only for the caller that flips {@code usedAt}
     * from null: a concurrent second presentation of the same token gets 0.
     */
    @Modifying(flushAutomatically = true)
    @Query("update RefreshToken rt set rt.usedAt = :now where rt.refreshTokenId = :id and rt.usedAt is null")
    int markUsed(@Param("id") UUID id, @Param("now") Instant now);
}
