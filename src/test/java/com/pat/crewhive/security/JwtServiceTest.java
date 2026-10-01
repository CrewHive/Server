package com.pat.crewhive.security;

import com.pat.crewhive.security.exception.custom.InvalidTokenException;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.util.Date;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for the claims written by {@link JwtService#generateToken}.
 */
class JwtServiceTest {

    private JwtService jwtService;
    private KeyPair keyPair;

    @BeforeEach
    void setUp() throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        keyPair = generator.generateKeyPair();
        jwtService = new JwtService(keyPair.getPrivate(), keyPair.getPublic());
    }

    @Test
    void generateToken_withoutCompany_hasNoCompanyIdClaim() {
        String token = jwtService.generateToken(
                UUID.randomUUID(), "mario.rossi@example.com", "Mario", "Rossi", Set.of("ROLE_USER"), null);

        Claims claims = jwtService.validateToken(token);

        assertThat(claims.containsKey("companyId")).isFalse();
    }

    @Test
    void generateToken_withCompany_hasCompanyIdClaim() {
        UUID companyId = UUID.randomUUID();

        String token = jwtService.generateToken(
                UUID.randomUUID(), "mario.rossi@example.com", "Mario", "Rossi", Set.of("ROLE_USER"), companyId);

        assertThat(jwtService.validateToken(token).get("companyId", String.class)).isEqualTo(companyId.toString());
    }

    @Test
    void generateToken_carriesIdentityClaimsAndFifteenMinutesLifetime() {
        UUID userId = UUID.randomUUID();

        Claims claims = jwtService.validateToken(jwtService.generateToken(
                userId, "mario.rossi@example.com", "Mario", "Rossi", Set.of("ROLE_USER"), null));

        assertThat(claims.getSubject()).isEqualTo(userId.toString());
        assertThat(claims.getId()).isNotBlank();
        assertThat(claims.get("email", String.class)).isEqualTo("mario.rossi@example.com");
        assertThat(claims.get("firstName", String.class)).isEqualTo("Mario");
        assertThat(claims.get("lastName", String.class)).isEqualTo("Rossi");
        assertThat(claims.get("role", String.class)).isEqualTo("ROLE_USER");
        long lifetime = claims.getExpiration().getTime() - claims.getIssuedAt().getTime();
        assertThat(lifetime).isEqualTo(JwtService.ACCESS_TOKEN_TTL_MILLIS);
    }

    @Test
    void generateToken_producesADifferentJtiEveryTime() {
        String first = jwtService.generateToken(UUID.randomUUID(), "a@example.com", "A", "B", Set.of(), null);
        String second = jwtService.generateToken(UUID.randomUUID(), "a@example.com", "A", "B", Set.of(), null);

        assertThat(jwtService.validateToken(first).getId()).isNotEqualTo(jwtService.validateToken(second).getId());
    }

    @Test
    void validateToken_rejectsAnExpiredToken() {
        String expired = Jwts.builder()
                .subject(UUID.randomUUID().toString())
                .issuedAt(new Date(System.currentTimeMillis() - 3_600_000))
                .expiration(new Date(System.currentTimeMillis() - 1_800_000))
                .signWith(keyPair.getPrivate(), Jwts.SIG.RS256)
                .compact();

        assertThatThrownBy(() -> jwtService.validateToken(expired))
                .isInstanceOf(InvalidTokenException.class)
                .hasMessageContaining("expired");
    }

    @Test
    void validateToken_rejectsATokenSignedWithAnotherKey() throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        KeyPair other = generator.generateKeyPair();
        String forged = new JwtService(other.getPrivate(), other.getPublic())
                .generateToken(UUID.randomUUID(), "a@example.com", "A", "B", Set.of("ROLE_DEV"), null);

        assertThatThrownBy(() -> jwtService.validateToken(forged)).isInstanceOf(InvalidTokenException.class);
    }

    @Test
    void validateToken_rejectsATamperedPayload() {
        String token = jwtService.generateToken(UUID.randomUUID(), "a@example.com", "A", "B", Set.of("ROLE_USER"), null);
        String[] parts = token.split("\\.");
        String tamperedPayload = java.util.Base64.getUrlEncoder().withoutPadding().encodeToString(
                "{\"sub\":\"x\",\"role\":\"ROLE_DEV\"}".getBytes());
        String tampered = parts[0] + "." + tamperedPayload + "." + parts[2];

        assertThatThrownBy(() -> jwtService.validateToken(tampered)).isInstanceOf(InvalidTokenException.class);
    }

    @Test
    void validateToken_rejectsAnUnsignedToken() {
        String unsigned = Jwts.builder().subject("x").compact();

        assertThatThrownBy(() -> jwtService.validateToken(unsigned)).isInstanceOf(InvalidTokenException.class);
    }

    @Test
    void validateToken_rejectsGarbage() {
        assertThatThrownBy(() -> jwtService.validateToken("not-a-jwt")).isInstanceOf(InvalidTokenException.class);
        assertThatThrownBy(() -> jwtService.validateToken("")).isInstanceOf(InvalidTokenException.class);
    }
}
