package com.pat.crewhive.security.filter;

import com.pat.crewhive.model.user.wrapper.CustomUserDetails;
import com.pat.crewhive.security.exception.custom.JwtAuthenticationException;
import com.pat.crewhive.service.JwtService;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;

import java.io.IOException;

@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;

    public JwtAuthenticationFilter(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) return true; // CORS preflight

        String uri = request.getRequestURI();
        // Whitelist SOLO per endpoint realmente pubblici
        return uri.equals("/api/auth/login")
                || uri.equals("/api/auth/register")
                || uri.equals("/api/auth/register/manager")
                || uri.equals("/api/auth/rotate")
                || uri.startsWith("/actuator/health")
                || uri.equals("/error")
                || uri.equals("/docs")
                || uri.startsWith("/docs/")
                || uri.equals("/swagger-ui.html")
                || uri.startsWith("/swagger-ui/");
        // NOTA: /api/auth/logout NON è escluso → il filtro gira e autentica
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain)
            throws ServletException, IOException {

        // Se già autenticato, prosegui
        if (SecurityContextHolder.getContext().getAuthentication() != null) {
            chain.doFilter(request, response);
            return;
        }

        String header = request.getHeader("Authorization");
        if (header == null || !header.startsWith("Bearer ")) {
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
            String username = claims.get("username", String.class);
            String role = claims.get("role", String.class);
            Long companyId = claims.get("companyId", Long.class); // opzionale se lo metti nei claim

            // Costruisci un CUD leggero dai claim (nessun accesso lazy)
            CustomUserDetails cud = CustomUserDetails.fromClaims(userId, username, role, companyId);

            UsernamePasswordAuthenticationToken auth =
                    new UsernamePasswordAuthenticationToken(cud, null, cud.getAuthorities());
            auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

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