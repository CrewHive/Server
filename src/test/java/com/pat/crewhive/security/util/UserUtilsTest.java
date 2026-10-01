package com.pat.crewhive.security.util;

import com.pat.crewhive.security.CustomUserDetails;
import com.pat.crewhive.user.UserDTO;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Date;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link UserUtils}.
 */
class UserUtilsTest {

    private static final UUID USER_ID = UUID.randomUUID();
    private static final UUID COMPANY_ID = UUID.randomUUID();

    private CustomUserDetails cud;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();
        cud = new CustomUserDetails(USER_ID, "mario.rossi@example.com", "Mario", "Rossi",
                Set.of("ROLE_USER", "ROLE_MANAGER"), COMPANY_ID, true, "jti-1", new Date());
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private void authenticateAs(Object principal) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null,
                        principal instanceof CustomUserDetails c ? c.getAuthorities() : AuthorityUtils.NO_AUTHORITIES));
    }

    // ---- nessuna autenticazione ----

    @Test
    void withoutAuthentication_everythingIsNullOrFalse() {
        assertThat(UserUtils.getAuthentication()).isNull();
        assertThat(UserUtils.isAuthenticated()).isFalse();
        assertThat(UserUtils.getCustomUserDetails()).isNull();
        assertThat(UserUtils.getCurrentUser()).isNull();
        assertThat(UserUtils.getCurrentUserId()).isNull();
        assertThat(UserUtils.getCurrentUsername()).isNull();
        assertThat(UserUtils.getCurrentUserRoles()).isNull();
        assertThat(UserUtils.hasRole("USER")).isFalse();
    }

    @Test
    void anonymousUser_isNotAuthenticated() {
        SecurityContextHolder.getContext().setAuthentication(new AnonymousAuthenticationToken(
                "key", "anonymousUser", AuthorityUtils.createAuthorityList("ROLE_ANONYMOUS")));

        assertThat(UserUtils.isAuthenticated()).isFalse();
        assertThat(UserUtils.getCustomUserDetails()).isNull();
    }

    @Test
    void principalOfAnotherType_isAuthenticatedButHasNoCustomUserDetails() {
        authenticateAs("some-string-principal");

        assertThat(UserUtils.isAuthenticated()).isTrue();
        assertThat(UserUtils.getCustomUserDetails()).isNull();
        assertThat(UserUtils.getCurrentUserId()).isNull();
        assertThat(UserUtils.hasRole("USER")).isFalse();
    }

    // ---- autenticato ----

    @Test
    void authenticated_exposesTheCurrentUserData() {
        authenticateAs(cud);

        assertThat(UserUtils.isAuthenticated()).isTrue();
        assertThat(UserUtils.getCustomUserDetails()).isSameAs(cud);
        assertThat(UserUtils.getCurrentUserId()).isEqualTo(USER_ID);
        assertThat(UserUtils.getCurrentUsername()).isEqualTo("Mario Rossi");
        assertThat(UserUtils.getCurrentUserRoles()).containsExactlyInAnyOrder("ROLE_USER", "ROLE_MANAGER");
    }

    @Test
    void getCurrentUser_mapsTheClaimsToAUserDto() {
        authenticateAs(cud);

        UserDTO dto = UserUtils.getCurrentUser();

        assertThat(dto).isNotNull();
        assertThat(dto.email()).isEqualTo("mario.rossi@example.com");
        assertThat(dto.firstName()).isEqualTo("Mario");
        assertThat(dto.lastName()).isEqualTo("Rossi");
        assertThat(dto.companyId()).isEqualTo(COMPANY_ID);
    }

    @Test
    void hasRole_acceptsBothPrefixedAndUnprefixedNames() {
        authenticateAs(cud);

        assertThat(UserUtils.hasRole("MANAGER")).isTrue();
        assertThat(UserUtils.hasRole("ROLE_MANAGER")).isTrue();
        assertThat(UserUtils.hasRole("DEV")).isFalse();
        assertThat(UserUtils.hasRole("ROLE_DEV")).isFalse();
    }
}
