package com.pat.crewhive.security.filter;

import com.pat.crewhive.model.user.wrapper.CustomUserDetails;
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
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final UserService userService;

    public JwtAuthenticationFilter(JwtService jwtService, UserService userService) {
        this.jwtService = jwtService;
        this.userService = userService;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {

        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) return true; // CORS preflight

        String uri = request.getRequestURI();
        return uri.startsWith("/api/auth/")
                || uri.startsWith("/actuator/health")
                || uri.equals("/error");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain)
            throws ServletException, IOException {

        // Se già autenticato, non rifare tutto
        if (SecurityContextHolder.getContext().getAuthentication() != null) {
            chain.doFilter(request, response);
            return;
        }

        String header = request.getHeader("Authorization");
        if (header == null || !header.startsWith("Bearer ")) {
            // Nessun token -> lascia proseguire: le rotte protette falliranno correttamente più avanti
            chain.doFilter(request, response);
            return;
        }

        String token = header.substring(7);
        try {

            Claims claims = jwtService.validateToken(token);
            String sub = claims.getSubject();

            if (sub == null) {

                log.warn("Invalid token subject");
                chain.doFilter(request, response);

                return;
            }

            Long userId = Long.parseLong(sub);
            User user = userService.getUserById(userId);
            CustomUserDetails userDetails = new CustomUserDetails(user);

            UsernamePasswordAuthenticationToken auth =
                    new UsernamePasswordAuthenticationToken(
                            userDetails, null, userDetails.getAuthorities());

            auth.setDetails(new org.springframework.security.web.authentication.WebAuthenticationDetailsSource()
                    .buildDetails(request));

            SecurityContextHolder.getContext().setAuthentication(auth);

        } catch (io.jsonwebtoken.JwtException e) {

            SecurityContextHolder.clearContext();
            log.warn("JWT validation error: {}", e.getMessage());

        } catch (RuntimeException e) {

            SecurityContextHolder.clearContext();
            log.error("Internal auth error: {}", e.getMessage());
        }

        chain.doFilter(request, response);
    }
}


