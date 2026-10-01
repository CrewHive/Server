package com.pat.crewhive.security;

import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for the claims written by {@link JwtService#generateToken}.
 */
class JwtServiceTest {

    private JwtService jwtService;

    @BeforeEach
    void setUp() throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        KeyPair keyPair = generator.generateKeyPair();
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
}
