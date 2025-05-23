package com.pat.crewhive.service;

import com.pat.crewhive.security.exception.custom.InvalidTokenException;
import com.pat.crewhive.model.user.entity.User;
import com.pat.crewhive.util.PemUtils;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.SignatureException;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.Date;

@Slf4j
@Service
public class JwtService {

    private PrivateKey privateKey;

    private PublicKey publicKey;

    @PostConstruct
    public void initKeys() {

        try {

            this.privateKey = PemUtils.loadPrivateKey("/static/private.pem");
            this.publicKey = PemUtils.loadPublicKey("/static/public.pem");

            log.info("Private Key: {}", privateKey);
            log.info("Public Key: {}", publicKey);

        } catch (Exception e) {

            log.error("Error loading keys: {}", e.getMessage());
            throw new IllegalStateException("Cannot load signature's keys", e);
        }
    }


    public String generateToken(User user) {

        String jwt = Jwts.builder()
                .setSubject(String.valueOf(user.getUserId()))
                .claim("role", user.getRole())
                .claim("username", user.getUsername())
                .setIssuedAt(new Date(System.currentTimeMillis()))
                .setExpiration(new Date(System.currentTimeMillis() + 1000 * 60 * 10))
                .signWith(privateKey, SignatureAlgorithm.RS256)
                .compact();

        log.info("Generated JWT token: {}", jwt);
        return jwt;
    }

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
