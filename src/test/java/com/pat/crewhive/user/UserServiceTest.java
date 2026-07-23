package com.pat.crewhive.user;

import com.pat.crewhive.authuser.AuthResponseDTO;
import com.pat.crewhive.authuser.RefreshTokenService;
import com.pat.crewhive.company.Company;
import com.pat.crewhive.security.JwtService;
import com.pat.crewhive.security.exception.custom.ResourceNotFoundException;
import com.pat.crewhive.common.PasswordUtil;
import com.pat.crewhive.common.StringUtils;
import com.pat.crewhive.shiftprogrammed.ShiftUserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link UserService}.
 */
@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private ShiftUserRepository shiftUserRepository;
    @Mock
    private PasswordUtil passwordUtil;
    @Mock
    private StringUtils stringUtils;
    @Mock
    private JwtService jwtService;
    @Mock
    private RefreshTokenService refreshTokenService;

    private UserService userService;

    private static final UUID USER_ID = UUID.randomUUID();
    private static final UUID COMPANY_ID = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        userService = new UserService(
                userRepository, shiftUserRepository, passwordUtil, stringUtils, jwtService, refreshTokenService
        );
    }

    private User buildUser(UUID userId, Company company) {
        User user = new User("mario.rossi@example.com", "Mario", "Rossi", "encoded-pwd");
        ReflectionTestUtils.setField(user, "userId", userId);
        user.setCompany(company);
        return user;
    }

    private Company buildCompany(UUID companyId, User... members) {
        Company company = new Company();
        ReflectionTestUtils.setField(company, "companyId", companyId);
        for (User member : members) {
            company.getUsers().add(member);
        }
        return company;
    }

    // ---------------------------------------------------------------------
    // leaveCompany()
    // ---------------------------------------------------------------------

    @Test
    void leaveCompany_reusesExistingRefreshToken_insteadOfDeletingAndRegeneratingIt() {
        Company company = buildCompany(COMPANY_ID);
        User user = buildUser(USER_ID, company);
        company.getUsers().add(user);

        when(userRepository.findById(USER_ID)).thenReturn(java.util.Optional.of(user));
        when(stringUtils.normalizeString(anyString())).thenReturn("mario.rossi@example.com");
        when(jwtService.generateToken(eq(USER_ID), anyString(), anyString(), anyString(), eq("ROLE_USER"), isNull()))
                .thenReturn("access-jwt");
        when(refreshTokenService.getOrIssueRefreshToken(user)).thenReturn("reused-refresh-token");

        AuthResponseDTO result = userService.leaveCompany(USER_ID);

        assertThat(result.getAccessToken()).isEqualTo("access-jwt");
        assertThat(result.getRefreshToken()).isEqualTo("reused-refresh-token");
        // the pre-existing refresh token must not be manually invalidated/regenerated here anymore
        verify(refreshTokenService, never()).deleteTokenByUser(any());
        verify(refreshTokenService, never()).generateRefreshToken(any());
    }

    @Test
    void leaveCompany_removesUserFromCompanyAndDeletesShifts() {
        Company company = buildCompany(COMPANY_ID);
        User user = buildUser(USER_ID, company);
        company.getUsers().add(user);

        when(userRepository.findById(USER_ID)).thenReturn(java.util.Optional.of(user));
        when(stringUtils.normalizeString(anyString())).thenReturn("mario.rossi@example.com");
        when(jwtService.generateToken(any(), anyString(), anyString(), anyString(), anyString(), any()))
                .thenReturn("access-jwt");
        when(refreshTokenService.getOrIssueRefreshToken(user)).thenReturn("reused-refresh-token");

        userService.leaveCompany(USER_ID);

        assertThat(user.getCompany()).isNull();
        assertThat(company.getUsers()).doesNotContain(user);
        verify(shiftUserRepository).deleteByUserId(USER_ID);
        verify(userRepository).save(user);
    }

    @Test
    void leaveCompany_throwsResourceNotFoundException_whenUserHasNoCompany() {
        User user = buildUser(USER_ID, null);

        when(userRepository.findById(USER_ID)).thenReturn(java.util.Optional.of(user));

        assertThatThrownBy(() -> userService.leaveCompany(USER_ID))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("User has no company");

        verifyNoInteractions(refreshTokenService, jwtService, shiftUserRepository);
    }
}
