package com.pat.crewhive.service;

import com.pat.crewhive.security.exception.custom.InvalidTokenException;
import com.pat.crewhive.model.user.entity.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.SignatureException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.Date;

@Slf4j
@Service
public class JwtService {

    private final PrivateKey privateKey;
    private final PublicKey publicKey;

    @Autowired
    public JwtService(PrivateKey privateKey,
                      PublicKey publicKey) {

        this.privateKey = privateKey;
        this.publicKey = publicKey;

        log.info("JWT Service initialized with private and public keys");
    }

    /**
     * Generates a JWT token for the given user details.
     *
     * @param userId   the ID of the user
     * @param username the username of the user
     * @param role     the role of the user
     * @return a JWT token as a String
     */
    public String generateToken(Long userId,
                                String username,
                                String role,
                                Long companyId) {

        String jwt = Jwts.builder()
                .setSubject(String.valueOf(userId))
                .claim("role", role)
                .claim("username", username)
                .claim("companyId", companyId)
                .setIssuedAt(new Date(System.currentTimeMillis()))
                .setExpiration(new Date(System.currentTimeMillis() + 1000 * 60 * 60))
                .signWith(privateKey, SignatureAlgorithm.RS256)
                .compact();

        log.info("Generated JWT token: {}", jwt);
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

            Claims claims = Jwts.parserBuilder()
                    .setSigningKey(publicKey)
                    .build()
                    .parseClaimsJws(token)
                    .getBody();

            log.info("Token is valid. Claims: {}", claims);
            return claims;

        } catch (ExpiredJwtException e) {

            log.error("Token expired on: {}", e.getClaims().getExpiration());
            throw new InvalidTokenException("Token expired on" + e.getClaims().getExpiration() + ". Login again to get a new token");

        } catch (SignatureException e) {

            log.error("Token's signature is not valid");
            throw new InvalidTokenException("Token's signature is not valid. Login again to get a new token");

        } catch (Exception e) {

            log.error("Token is not valid");
            throw new InvalidTokenException("Token is not valid. Login again to get a new token");
        }
    }

}
