package com.pat.crewhive.security.filter;

import com.pat.crewhive.security.CustomUserDetails;
import com.pat.crewhive.security.JwtService;
import com.pat.crewhive.security.TokenBlackListService;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.jspecify.annotations.NonNull;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;

import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.net.URI;
import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.Date;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(JwtAuthenticationFilter.class);

    private final JwtService jwtService;
    private final TokenBlackListService tokenBlackListService;
    private final ObjectMapper objectMapper;

    public JwtAuthenticationFilter(JwtService jwtService,
                                   TokenBlackListService tokenBlackListService,
                                   ObjectMapper objectMapper) {
        this.jwtService = jwtService;
        this.tokenBlackListService = tokenBlackListService;
        this.objectMapper = objectMapper;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) return true; // CORS preflight

        String uri = request.getRequestURI();
        // Whitelist SOLO per endpoint realmente pubblici
        return uri.equals("/api/auth/login")
                || uri.equals("/api/auth/register")
                || uri.equals("/api/auth/rotate")
                || uri.startsWith("/actuator/health")
                || uri.equals("/error");
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
                log.warn("Token has been revoked: jti={}", jti);
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

            if (tokenBlackListService.isRevokedForUser(userId, claims.getIssuedAt())) {
                log.warn("Token issued before the user's tokens were revoked: userId={}, jti={}", userId, jti);
                SecurityContextHolder.clearContext();
                chain.doFilter(request, response);
                return;
            }

            String email = claims.get("email").toString();
            String firstName = claims.get("firstName", String.class);
            String lastName = claims.get("lastName", String.class);
            String roleClaim = claims.get("role", String.class);
            Set<String> roles = (roleClaim == null || roleClaim.isBlank())
                    ? Set.of()
                    : Arrays.stream(roleClaim.split(",")).collect(Collectors.toSet());
            String companyIdClaim = claims.get("companyId", String.class);
            UUID companyId = companyIdClaim != null ? UUID.fromString(companyIdClaim) : null;
            Date tokenExpiration = claims.getExpiration();

            // Costruisci un CUD leggero dai claim (nessun accesso lazy)
            CustomUserDetails cud = CustomUserDetails.fromClaims(userId, email,firstName, lastName, roles, companyId, jti, tokenExpiration);

            UsernamePasswordAuthenticationToken auth =
                    new UsernamePasswordAuthenticationToken(cud, null, cud.getAuthorities());
            auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

            SecurityContextHolder.getContext().setAuthentication(auth);

        } catch (io.jsonwebtoken.JwtException e) {
            SecurityContextHolder.clearContext();
            log.warn("JWT validation error: {}", e.getMessage());
        } catch (DataAccessException e) {
            // Redis (blacklist) non raggiungibile: restiamo fail-closed, ma rispondiamo 503 e non 401,
            // cosi' il client non scarta un token che non e' invalido.
            SecurityContextHolder.clearContext();
            log.error("Token revocation store unavailable, request rejected", e);
            writeServiceUnavailable(request, response);
            return;
        } catch (RuntimeException e) {
            SecurityContextHolder.clearContext();
            log.error("Internal auth error: {}", e.getMessage());
        }

        chain.doFilter(request, response);
    }

    private void writeServiceUnavailable(HttpServletRequest request, HttpServletResponse response) throws IOException {

        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.SERVICE_UNAVAILABLE, "Service temporarily unavailable");
        pd.setTitle("Service Unavailable");
        pd.setType(URI.create("about:blank"));
        pd.setProperty("timestamp", OffsetDateTime.now().toString());
        pd.setProperty("path", request.getRequestURI());
        pd.setProperty("errorCode", "AUTH_503_UNAVAILABLE");

        response.setStatus(HttpStatus.SERVICE_UNAVAILABLE.value());
        response.setHeader(HttpHeaders.RETRY_AFTER, "5");
        response.setContentType("application/json; charset=UTF-8");
        objectMapper.writeValue(response.getWriter(), pd);
    }
}
