package com.pat.crewhive.security;

import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;

import java.util.Date;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link CustomUserDetails}.
 */
class CustomUserDetailsTest {

    private static final UUID USER_ID = UUID.randomUUID();
    private static final UUID COMPANY_ID = UUID.randomUUID();

    @Test
    void fromClaims_buildsAnEnabledPrincipalWithTheClaimValues() {
        Date exp = new Date(System.currentTimeMillis() + 60_000);

        CustomUserDetails cud = CustomUserDetails.fromClaims(
                USER_ID, "mario.rossi@example.com", "Mario", "Rossi", Set.of("ROLE_USER"), COMPANY_ID, "jti-1", exp);

        assertThat(cud.getUserId()).isEqualTo(USER_ID);
        assertThat(cud.getEmail()).isEqualTo("mario.rossi@example.com");
        assertThat(cud.getFirstName()).isEqualTo("Mario");
        assertThat(cud.getLastName()).isEqualTo("Rossi");
        assertThat(cud.getRoles()).containsExactly("ROLE_USER");
        assertThat(cud.getCompanyId()).isEqualTo(COMPANY_ID);
        assertThat(cud.getJti()).isEqualTo("jti-1");
        assertThat(cud.getTokenExpiration()).isEqualTo(exp);
        assertThat(cud.isEnabled()).isTrue();
    }

    @Test
    void usernameIsFirstAndLastName_andThereIsNoPassword() {
        CustomUserDetails cud = CustomUserDetails.fromClaims(
                USER_ID, "m@example.com", "Mario", "Rossi", Set.of(), null, "jti", new Date());

        assertThat(cud.getUsername()).isEqualTo("Mario Rossi");
        assertThat(cud.getPassword()).isNull();
    }

    @Test
    void authoritiesMirrorTheRoles() {
        CustomUserDetails cud = CustomUserDetails.fromClaims(
                USER_ID, "m@example.com", "Mario", "Rossi", Set.of("ROLE_USER", "ROLE_MANAGER"), COMPANY_ID, "jti", new Date());

        assertThat(cud.getAuthorities()).extracting(GrantedAuthority::getAuthority)
                .containsExactlyInAnyOrder("ROLE_USER", "ROLE_MANAGER");
    }

    @Test
    void accountFlags_areAlwaysNonExpiredNonLockedAndCredentialsValid() {
        CustomUserDetails cud = CustomUserDetails.fromClaims(
                USER_ID, "m@example.com", "Mario", "Rossi", Set.of(), null, "jti", new Date());

        assertThat(cud.isAccountNonExpired()).isTrue();
        assertThat(cud.isAccountNonLocked()).isTrue();
        assertThat(cud.isCredentialsNonExpired()).isTrue();
    }

    @Test
    void constructor_withWorkingFalse_isDisabled() {
        CustomUserDetails cud = new CustomUserDetails(
                USER_ID, "m@example.com", "Mario", "Rossi", Set.of(), null, false, "jti", new Date());

        assertThat(cud.isEnabled()).isFalse();
        assertThat(cud.getCompanyId()).isNull();
    }
}
