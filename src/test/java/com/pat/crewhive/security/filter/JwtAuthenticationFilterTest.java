package com.pat.crewhive.security.filter;

import com.pat.crewhive.security.CustomUserDetails;
import com.pat.crewhive.security.JwtService;
import com.pat.crewhive.security.TokenBlackListService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import io.jsonwebtoken.Jwts;
import org.springframework.dao.QueryTimeoutException;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import tools.jackson.databind.ObjectMapper;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.util.Date;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link JwtAuthenticationFilter}: blacklist per jti and per user (M5).
 */
@ExtendWith(MockitoExtension.class)
class JwtAuthenticationFilterTest {

    private static final UUID USER_ID = UUID.randomUUID();
    private static final UUID COMPANY_ID = UUID.randomUUID();

    @Mock
    private TokenBlackListService tokenBlackListService;

    private JwtService jwtService;
    private KeyPair keyPair;
    private JwtAuthenticationFilter filter;

    @BeforeEach
    void setUp() throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        keyPair = generator.generateKeyPair();
        jwtService = new JwtService(keyPair.getPrivate(), keyPair.getPublic());
        filter = new JwtAuthenticationFilter(jwtService, tokenBlackListService, new ObjectMapper());
        SecurityContextHolder.clearContext();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private MockHttpServletResponse response;
    private MockFilterChain chain;

    private Authentication runFilterWithFreshToken() throws Exception {
        return runFilterWithFreshToken(COMPANY_ID);
    }

    private Authentication runFilterWithFreshToken(UUID companyId) throws Exception {
        String token = jwtService.generateToken(
                USER_ID, "mario.rossi@example.com", "Mario", "Rossi", Set.of("ROLE_USER"), companyId);

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/user/me");
        request.addHeader("Authorization", "Bearer " + token);
        response = new MockHttpServletResponse();
        chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        return SecurityContextHolder.getContext().getAuthentication();
    }

    @Test
    void validToken_ofNonRevokedUser_authenticates() throws Exception {
        when(tokenBlackListService.isRevoked(any())).thenReturn(false);
        when(tokenBlackListService.isRevokedForUser(eq(USER_ID), any(Date.class))).thenReturn(false);

        Authentication auth = runFilterWithFreshToken();

        assertThat(auth).isNotNull();
        assertThat(((CustomUserDetails) auth.getPrincipal()).getUserId()).isEqualTo(USER_ID);
    }

    @Test
    void validToken_ofUserWithRevokedTokens_doesNotAuthenticate() throws Exception {
        when(tokenBlackListService.isRevoked(any())).thenReturn(false);
        when(tokenBlackListService.isRevokedForUser(eq(USER_ID), any(Date.class))).thenReturn(true);

        assertThat(runFilterWithFreshToken()).isNull();
    }

    @Test
    void validToken_withRevokedJti_doesNotAuthenticate() throws Exception {
        when(tokenBlackListService.isRevoked(any())).thenReturn(true);

        assertThat(runFilterWithFreshToken()).isNull();
    }

    @Test
    void redisUnavailable_answers503AndDoesNotContinueTheChain() throws Exception {
        when(tokenBlackListService.isRevoked(any())).thenThrow(new QueryTimeoutException("redis down"));

        Authentication auth = runFilterWithFreshToken();

        assertThat(auth).isNull();
        assertThat(response.getStatus()).isEqualTo(503);
        assertThat(response.getHeader("Retry-After")).isNotNull();
        assertThat(response.getContentAsString()).contains("AUTH_503_UNAVAILABLE");
        assertThat(chain.getRequest()).isNull();
    }

    @Test
    void validToken_ofUserWithoutCompany_authenticatesWithNullCompanyId() throws Exception {
        when(tokenBlackListService.isRevoked(any())).thenReturn(false);
        when(tokenBlackListService.isRevokedForUser(eq(USER_ID), any(Date.class))).thenReturn(false);

        Authentication auth = runFilterWithFreshToken(null);

        assertThat(auth).isNotNull();
        assertThat(((CustomUserDetails) auth.getPrincipal()).getCompanyId()).isNull();
    }

    // ---------------------------------------------------------------------
    // shouldNotFilter / header handling
    // ---------------------------------------------------------------------

    @ParameterizedTest
    @ValueSource(strings = {"/api/auth/login", "/api/auth/register", "/api/auth/rotate", "/actuator/health", "/actuator/health/liveness", "/error"})
    void publicPaths_areNotFiltered(String path) {
        assertThat(filter.shouldNotFilter(new MockHttpServletRequest("POST", path))).isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = {"/api/user/me", "/api/auth/logout", "/api/auth/verify-email", "/manager/create-role", "/docs"})
    void protectedPaths_areFiltered(String path) {
        assertThat(filter.shouldNotFilter(new MockHttpServletRequest("GET", path))).isFalse();
    }

    @Test
    void corsPreflight_isNeverFiltered() {
        assertThat(filter.shouldNotFilter(new MockHttpServletRequest("OPTIONS", "/api/user/me"))).isTrue();
    }

    private MockFilterChain runWithHeader(String authorization) throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/user/me");
        if (authorization != null) request.addHeader("Authorization", authorization);
        response = new MockHttpServletResponse();
        chain = new MockFilterChain();
        filter.doFilter(request, response, chain);
        return chain;
    }

    @Test
    void missingAuthorizationHeader_continuesUnauthenticated() throws Exception {
        runWithHeader(null);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        assertThat(chain.getRequest()).isNotNull();
        verifyNoInteractions(tokenBlackListService);
    }

    @Test
    void nonBearerAuthorizationHeader_continuesUnauthenticated() throws Exception {
        runWithHeader("Basic dXNlcjpwYXNz");

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        assertThat(chain.getRequest()).isNotNull();
        verifyNoInteractions(tokenBlackListService);
    }

    @Test
    void garbageBearerToken_continuesUnauthenticated() throws Exception {
        runWithHeader("Bearer not-a-jwt");

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        assertThat(chain.getRequest()).isNotNull();
    }

    @Test
    void alreadyAuthenticatedRequest_isLeftUntouched() throws Exception {
        Authentication existing = new org.springframework.security.authentication.UsernamePasswordAuthenticationToken("someone", null);
        SecurityContextHolder.getContext().setAuthentication(existing);

        runWithHeader("Bearer not-a-jwt");

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isSameAs(existing);
        verifyNoInteractions(tokenBlackListService);
    }

    // ---------------------------------------------------------------------
    // claims mancanti o malformati
    // ---------------------------------------------------------------------

    private String signed(io.jsonwebtoken.JwtBuilder builder) {
        return builder.id(UUID.randomUUID().toString())
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + 60_000))
                .signWith(keyPair.getPrivate(), Jwts.SIG.RS256)
                .compact();
    }

    @Test
    void tokenWithoutSubject_doesNotAuthenticate() throws Exception {
        when(tokenBlackListService.isRevoked(any())).thenReturn(false);

        runWithHeader("Bearer " + signed(Jwts.builder().claim("email", "a@example.com")));

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        assertThat(chain.getRequest()).isNotNull();
    }

    @Test
    void tokenWithNonUuidSubject_doesNotAuthenticate() throws Exception {
        when(tokenBlackListService.isRevoked(any())).thenReturn(false);

        runWithHeader("Bearer " + signed(Jwts.builder().subject("not-a-uuid").claim("email", "a@example.com")));

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        assertThat(chain.getRequest()).isNotNull();
    }

    @Test
    void tokenWithoutEmailClaim_doesNotAuthenticate() throws Exception {
        when(tokenBlackListService.isRevoked(any())).thenReturn(false);
        when(tokenBlackListService.isRevokedForUser(eq(USER_ID), any(Date.class))).thenReturn(false);

        runWithHeader("Bearer " + signed(Jwts.builder().subject(USER_ID.toString())));

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        assertThat(chain.getRequest()).isNotNull();
    }

    @Test
    void tokenWithoutRoleClaim_authenticatesWithNoAuthorities() throws Exception {
        when(tokenBlackListService.isRevoked(any())).thenReturn(false);
        when(tokenBlackListService.isRevokedForUser(eq(USER_ID), any(Date.class))).thenReturn(false);

        runWithHeader("Bearer " + signed(Jwts.builder().subject(USER_ID.toString()).claim("email", "a@example.com")));

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        assertThat(auth).isNotNull();
        assertThat(auth.getAuthorities()).isEmpty();
    }

    @Test
    void multipleRolesInTheClaim_becomeSeparateAuthorities() throws Exception {
        when(tokenBlackListService.isRevoked(any())).thenReturn(false);
        when(tokenBlackListService.isRevokedForUser(eq(USER_ID), any(Date.class))).thenReturn(false);

        runWithHeader("Bearer " + signed(Jwts.builder().subject(USER_ID.toString())
                .claim("email", "a@example.com").claim("role", "ROLE_USER,ROLE_MANAGER")));

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        assertThat(auth.getAuthorities()).extracting(Object::toString).containsExactlyInAnyOrder("ROLE_USER", "ROLE_MANAGER");
    }
}
