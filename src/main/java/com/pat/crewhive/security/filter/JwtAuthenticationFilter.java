package com.pat.crewhive.security.filter;

import com.pat.crewhive.dto.UserDTO;
import com.pat.crewhive.model.user.entity.User;
import com.pat.crewhive.security.exception.custom.JwtAuthenticationException;
import com.pat.crewhive.service.JwtService;
import com.pat.crewhive.service.UserService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final UserService userService;

    public JwtAuthenticationFilter(JwtService jwtService,
                                   UserService userService) {
        this.jwtService = jwtService;
        this.userService = userService;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getServletPath();
        // salta login e refresh senza matcher esterni
        return "/api/auth/login".equals(path)
                || "/api/auth/refresh".equals(path);
    }


    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain)
            throws ServletException, IOException {

        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            String token = header.substring(7);
            try {
                // 1) validateToken ritorna un io.jsonwebtoken.Claims
                Claims claims = jwtService.validateToken(token);

                // 2) estrai subject come Long
                Long userId = Long.valueOf(claims.getSubject());

                // 3) carica il DTO utente
                User user = userService.getUserById(userId);

                // 4) costruisci l’Authentication
                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(
                                user,
                                null,
                                List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole()))
                        );

                SecurityContextHolder.getContext().setAuthentication(authentication);

            } catch (JwtException ex) {
                SecurityContextHolder.clearContext();
                throw new JwtAuthenticationException(ex.getMessage());

            } catch (RuntimeException ex) {
                SecurityContextHolder.clearContext();
                throw new JwtAuthenticationException("Errore interno di autenticazione");
            }
        }

        chain.doFilter(request, response);
    }

}

