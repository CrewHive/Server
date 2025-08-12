package com.pat.crewhive.security.filter;

import com.pat.crewhive.model.user.entity.CustomUserDetails;
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
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Slf4j
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

        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) return true; // CORS preflight

        return path.startsWith("/api/auth");
    }


    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain)
            throws ServletException, IOException {

        String header = request.getHeader("Authorization");

        // <— NON rifare il lavoro se già autenticato
        if (SecurityContextHolder.getContext().getAuthentication() == null
                && header != null && header.startsWith("Bearer ")) {

            String token = header.substring(7);

            log.debug("Processing JWT token");

            try {

                Claims claims = jwtService.validateToken(token);
                String sub = claims.getSubject();
                if (sub == null) throw new JwtAuthenticationException("Invalid token subject");

                Long userId = Long.parseLong(sub);

                // Carica dal DB
                User user = userService.getUserById(userId);
                CustomUserDetails userDetails = new CustomUserDetails(user);

                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(
                                userDetails, null, userDetails.getAuthorities()
                        );

                // <— aggiungi dettagli della request
                authentication.setDetails(
                        new org.springframework.security.web.authentication.WebAuthenticationDetailsSource()
                                .buildDetails(request)
                );

                SecurityContextHolder.getContext().setAuthentication(authentication);

            } catch (JwtException ex) {

                SecurityContextHolder.clearContext();
                log.error("JWT validation error: {}", ex.getMessage());
                throw new JwtAuthenticationException(ex.getMessage());

            } catch (RuntimeException ex) {

                SecurityContextHolder.clearContext();
                log.error("Internal authentication error: {}", ex.getMessage());
                throw new JwtAuthenticationException("Errore interno di autenticazione");
            }
        }

        chain.doFilter(request, response);
    }

}

