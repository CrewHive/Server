package com.pat.crewhive.company;

import com.pat.crewhive.authuser.AuthResponseDTO;
import com.pat.crewhive.authuser.RefreshTokenService;
import com.pat.crewhive.manager.Role;
import com.pat.crewhive.manager.RoleRepository;
import com.pat.crewhive.manager.UserRole;
import com.pat.crewhive.security.JwtService;
import com.pat.crewhive.security.exception.custom.ResourceAlreadyExistsException;
import com.pat.crewhive.common.StringUtils;
import com.pat.crewhive.user.User;
import com.pat.crewhive.user.UserService;
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
 * Unit tests for {@link CompanyService}.
 */
@ExtendWith(MockitoExtension.class)
class CompanyServiceTest {

    @Mock
    private CompanyRepository companyRepository;
    @Mock
    private UserService userService;
    @Mock
    private JwtService jwtService;
    @Mock
    private RoleRepository roleRepository;
    @Mock
    private StringUtils stringUtils;
    @Mock
    private RefreshTokenService refreshTokenService;
    @Mock
    private CompanyAccessService companyAccessService;

    private CompanyService companyService;

    @BeforeEach
    void setUp() {
        companyService = new CompanyService(
                companyRepository, userService, stringUtils, roleRepository, jwtService, refreshTokenService, companyAccessService
        );
    }

    private User buildManager(UUID userId) {
        User user = new User("manager@example.com", "Mario", "Rossi", "encoded-pwd");
        ReflectionTestUtils.setField(user, "userId", userId);
        Role role = new Role("ROLE_USER", null);
        user.setRole(new UserRole(user, role));
        return user;
    }

    // ---------------------------------------------------------------------
    // registerCompany()
    // ---------------------------------------------------------------------

    @Test
    void registerCompany_reusesExistingRefreshToken_insteadOfDeletingIt() {
        CompanyRegistrationDTO request = new CompanyRegistrationDTO("Acme", CompanyType.RESTAURANT, null);
        UUID managerId = UUID.randomUUID();
        User manager = buildManager(managerId);
        Role managerRole = new Role("ROLE_MANAGER", null);

        when(stringUtils.normalizeString("Acme")).thenReturn("acme");
        when(companyRepository.existsByName("acme")).thenReturn(false);
        when(userService.getUserById(managerId)).thenReturn(manager);
        when(roleRepository.findByRoleNameIgnoreCaseAndCompanyIsNull("ROLE_MANAGER"))
                .thenReturn(java.util.Optional.of(managerRole));
        when(stringUtils.normalizeString("manager@example.com")).thenReturn("manager@example.com");
        when(jwtService.generateToken(eq(managerId), anyString(), anyString(), anyString(), eq("ROLE_MANAGER"), any()))
                .thenReturn("access-jwt");
        when(refreshTokenService.getOrIssueRefreshToken(manager)).thenReturn("reused-refresh-token");

        AuthResponseDTO result = companyService.registerCompany(managerId, request);

        assertThat(result.getAccessToken()).isEqualTo("access-jwt");
        assertThat(result.getRefreshToken()).isEqualTo("reused-refresh-token");
        // registering a company must not manually invalidate/regenerate the session's refresh token anymore
        verify(refreshTokenService, never()).deleteTokenByUser(any());
        verify(refreshTokenService, never()).generateRefreshToken(any());
    }

    @Test
    void registerCompany_throwsResourceAlreadyExistsException_whenNameIsTaken() {
        CompanyRegistrationDTO request = new CompanyRegistrationDTO("Acme", CompanyType.RESTAURANT, null);

        when(stringUtils.normalizeString("Acme")).thenReturn("acme");
        when(companyRepository.existsByName("acme")).thenReturn(true);

        assertThatThrownBy(() -> companyService.registerCompany(UUID.randomUUID(), request))
                .isInstanceOf(ResourceAlreadyExistsException.class);

        verifyNoInteractions(userService, jwtService, refreshTokenService);
    }
}
