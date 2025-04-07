package com.pat.hours_calculator.service;

import com.pat.hours_calculator.exception.custom.InvalidTokenException;
import com.pat.hours_calculator.model.user.entity.User;
import com.pat.hours_calculator.util.PemUtils;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.SignatureException;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Service;

import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.Date;

@Service
public class JwtService {

    private PrivateKey privateKey;

    private PublicKey publicKey;

    @PostConstruct
    public void initKeys() {

        try {

            PrivateKey privateKey = PemUtils.loadPrivateKey("/static/private.pem");
            PublicKey publicKey = PemUtils.loadPublicKey("/static/public.pem");

        } catch (Exception e) {

            throw new IllegalStateException("Cannot load signature's keys", e);
        }
    }


    public String generateToken(User user) {

        return Jwts.builder()
                .setSubject(String.valueOf(user.getUser_id()))
                .claim("role", user.getRole())
                .claim("username", user.getUsername())
                .setIssuedAt(new Date(System.currentTimeMillis()))
                .setExpiration(new Date(System.currentTimeMillis() + 1000 * 60 * 10))
                .signWith(privateKey, SignatureAlgorithm.RS256)
                .compact();
    }

    public Claims validateToken(String token) {

        try {

            return Jwts.parserBuilder()
                    .setSigningKey(publicKey)
                    .build()
                    .parseClaimsJws(token)
                    .getBody();

        } catch (ExpiredJwtException e) {

            throw new InvalidTokenException("Token expired on" + e.getClaims().getExpiration() + ". Login again to get a new token");

        } catch (SignatureException e) {

            throw new InvalidTokenException("Token's signature is not valid. Login again to get a new token");

        } catch (Exception e) {

            throw new InvalidTokenException("Token is not valid. Login again to get a new token");
        }
    }

}
