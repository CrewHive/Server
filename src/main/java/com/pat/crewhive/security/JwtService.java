package com.pat.crewhive.security;

import com.pat.crewhive.security.exception.custom.InvalidTokenException;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtBuilder;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.SignatureException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.Date;
import java.util.Set;
import java.util.UUID;

@Service
public class JwtService {

    private static final Logger log = LoggerFactory.getLogger(JwtService.class);

    public static final long ACCESS_TOKEN_TTL_MILLIS = 1000 * 60 * 15; // 15 minuti
    private static final long CLOCK_SKEW_SECONDS = 30; // tolleranza di sincronizzazione oraria

    private final PrivateKey privateKey;
    private final PublicKey publicKey;

    public JwtService(PrivateKey privateKey,
                      PublicKey publicKey) {

        this.privateKey = privateKey;
        this.publicKey = publicKey;

        log.info("JWT Service initialized with private and public keys");
    }

    /**
     * Generates a JWT token for the given user details.
     *
     * @param userId    the ID of the user
     * @param email     the email of the user
     * @param firstName the first name of the user
     * @param lastName  the last name of the user
     * @param roles     the set of the roles of the user
     * @param companyId the company of the user
     * @return a JWT token as a String
     */
    public String generateToken(UUID userId,
                                String email,
                                String firstName,
                                String lastName,
                                Set<String> roles,
                                UUID companyId) {

        Date now = new Date();

        JwtBuilder builder = Jwts.builder()
                .id(UUID.randomUUID().toString())
                .subject(String.valueOf(userId))
                .claim("role", String.join(",", roles)) // ROLE_USER,ROLE_MANAGER, ...
                .claim("email", email)
                .claim("firstName", firstName)
                .claim("lastName", lastName)
                .issuedAt(now)
                .expiration(new Date(now.getTime() + ACCESS_TOKEN_TTL_MILLIS));

        // Senza company il claim non c'e': String.valueOf(null) scriverebbe la stringa "null",
        // che il filtro non riesce a convertire in UUID.
        if (companyId != null) {
            builder.claim("companyId", companyId.toString());
        }

        String jwt = builder.signWith(privateKey, Jwts.SIG.RS256).compact();

        log.info("Generated JWT token for userId={}", userId);
        return jwt;
    }

    /**
     * Validates the given JWT token and returns the claims if valid.
     *
     * @param token the JWT token to validate
     * @return the claims contained in the token
     * @throws InvalidTokenException if the token is invalid or expired
     */
    public Claims validateToken(String token) {

        try {

            Claims claims = Jwts.parser()
                    .verifyWith(publicKey)
                    .clockSkewSeconds(CLOCK_SKEW_SECONDS)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            log.debug("Token valid: jti={}", claims.getId());
            return claims;

        } catch (ExpiredJwtException e) {

            log.error("Token expired on: {}", e.getClaims().getExpiration());
            throw new InvalidTokenException("Token expired on " + e.getClaims().getExpiration() + ". Login again to get a new token");

        } catch (SignatureException e) {

            log.error("Token's signature is not valid");
            throw new InvalidTokenException("Token's signature is not valid. Login again to get a new token");

        } catch (Exception e) {

            log.error("Token is not valid", e);
            throw new InvalidTokenException("Token is not valid. Login again to get a new token");
        }
    }

}