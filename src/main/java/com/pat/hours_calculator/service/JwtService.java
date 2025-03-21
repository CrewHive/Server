package com.pat.hours_calculator.service;

import com.pat.hours_calculator.model.user.entities.User;
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
            e.printStackTrace();//todo: handle better the exception
        }
    }


    public String generateToken(User user) {

        return Jwts.builder()
                .setSubject(String.valueOf(user.getUserId()))
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

            System.out.println("Token scaduto il: " + e.getClaims().getExpiration());
            throw e;

        } catch (SignatureException e) {

            System.out.println("Firma del token non valida.");
            throw e;

        } catch (Exception e) {

            System.out.println("Errore durante la validazione del token.");
            throw e;
        }
    }

}
