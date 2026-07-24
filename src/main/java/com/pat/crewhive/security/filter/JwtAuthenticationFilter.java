package com.pat.crewhive.security.filter;

import com.pat.crewhive.security.CustomUserDetails;
import com.pat.crewhive.security.JwtService;
import com.pat.crewhive.security.TokenBlackListService;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;

import java.io.IOException;
import java.util.Date;
import java.util.UUID;

@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final TokenBlackListService tokenBlackListService;

    public JwtAuthenticationFilter(JwtService jwtService,
                                   TokenBlackListService tokenBlackListService) {
        this.jwtService = jwtService;
        this.tokenBlackListService = tokenBlackListService;
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
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain chain)
            throws ServletException, IOException {

        // Se già autenticato, prosegui
        if (SecurityContextHolder.getContext().getAuthentication() != null) {
            chain.doFilter(request, response);
            return;
        }

        String header = request.getHeader("Authorization");
        if (header == null || !header.startsWith("Bearer ")) {
            SecurityContextHolder.clearContext();
            chain.doFilter(request, response);
            return;
        }

        String token = header.substring(7);
        try {
            Claims claims = jwtService.validateToken(token);

            String jti = claims.getId();

            if (tokenBlackListService.isRevoked(jti)) {
                log.warn("token has been revoked {}", claims.getSubject());
                SecurityContextHolder.clearContext();
                chain.doFilter(request, response);
                return;
            }

            String sub = claims.getSubject();
            if (sub == null) {
                log.warn("Invalid token subject");
                SecurityContextHolder.clearContext();
                chain.doFilter(request, response);
                return;
            }

            UUID userId = UUID.fromString(sub);
            String email = claims.get("email").toString();
            String firstName = claims.get("firstName", String.class);
            String lastName = claims.get("lastName", String.class);
            String role = claims.get("role", String.class);
            String companyIdClaim = claims.get("companyId", String.class);
            UUID companyId = companyIdClaim != null ? UUID.fromString(companyIdClaim) : null;
            Date tokenExpiration = claims.getExpiration();

            // Costruisci un CUD leggero dai claim (nessun accesso lazy)
            CustomUserDetails cud = CustomUserDetails.fromClaims(userId, email,firstName, lastName, role, companyId, jti, tokenExpiration);

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