package com.pat.crewhive.company;

import com.pat.crewhive.authuser.AuthResponseDTO;
import com.pat.crewhive.authuser.RefreshTokenService;
import com.pat.crewhive.manager.Role;
import com.pat.crewhive.manager.RoleRepository;
import com.pat.crewhive.manager.UserRole;
import com.pat.crewhive.security.JwtService;
import com.pat.crewhive.security.exception.custom.ResourceAlreadyExistsException;
import com.pat.crewhive.shifttemplate.ShiftTemplateRepository;
import com.pat.crewhive.common.StringUtils;
import com.pat.crewhive.user.User;
import com.pat.crewhive.security.exception.custom.ResourceNotFoundException;
import com.pat.crewhive.user.UserService;
import com.pat.crewhive.user.UserWithTimeParamsDTO;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
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
    @Mock
    private ShiftTemplateRepository shiftTemplateRepository;

    private CompanyService companyService;

    @BeforeEach
    void setUp() {
        companyService = new CompanyService(
                companyRepository, userService, stringUtils, roleRepository, jwtService, refreshTokenService, companyAccessService, shiftTemplateRepository
        );
    }

    private User buildManager(UUID userId) {
        User user = new User("manager@example.com", "Mario", "Rossi", "encoded-pwd");
        ReflectionTestUtils.setField(user, "userId", userId);
        Role role = new Role("ROLE_USER", null);
        user.addRole(role);
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
        when(jwtService.generateToken(eq(managerId), anyString(), anyString(), anyString(), any(), any()))
                .thenReturn("access-jwt");
        when(refreshTokenService.getOrIssueRefreshToken(manager)).thenReturn("reused-refresh-token");

        AuthResponseDTO result = companyService.registerCompany(managerId, request);

        assertThat(result.accessToken()).isEqualTo("access-jwt");
        assertThat(result.refreshToken()).isEqualTo("reused-refresh-token");
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

    // ---------------------------------------------------------------------
    // deleteCompany()
    // ---------------------------------------------------------------------

    // ---------------------------------------------------------------------
    // assertCanReadCompanyUser() / getCompanyUserWithInformation()  (H2)
    // ---------------------------------------------------------------------

    private Company companyWithId(UUID companyId) {
        Company c = new Company();
        ReflectionTestUtils.setField(c, "companyId", companyId);
        return c;
    }

    private User userIn(UUID userId, Company company) {
        User u = new User("u-" + userId + "@example.com", "Luigi", "Verdi", "encoded-pwd");
        ReflectionTestUtils.setField(u, "userId", userId);
        u.setCompany(company);
        return u;
    }

    @Test
    void assertCanReadCompanyUser_managerAndTargetInCompany_passes() {
        UUID companyId = UUID.randomUUID();
        UUID managerId = UUID.randomUUID();
        User target = userIn(UUID.randomUUID(), companyWithId(companyId));

        when(companyAccessService.isNotPartOfCompany(managerId, companyId)).thenReturn(false);
        when(userService.getUserById(target.getUserId())).thenReturn(target);

        assertThatCode(() -> companyService.assertCanReadCompanyUser(managerId, companyId, target.getUserId()))
                .doesNotThrowAnyException();
    }

    @Test
    void assertCanReadCompanyUser_managerNotInCompany_throwsAuthorizationDenied() {
        UUID companyId = UUID.randomUUID();
        UUID managerId = UUID.randomUUID();

        when(companyAccessService.isNotPartOfCompany(managerId, companyId)).thenReturn(true);

        assertThatThrownBy(() -> companyService.assertCanReadCompanyUser(managerId, companyId, UUID.randomUUID()))
                .isInstanceOf(AuthorizationDeniedException.class);
        verifyNoInteractions(userService);
    }

    @Test
    void assertCanReadCompanyUser_targetInOtherCompanyOrNone_throwsResourceNotFound() {
        UUID companyId = UUID.randomUUID();
        UUID managerId = UUID.randomUUID();
        User foreign = userIn(UUID.randomUUID(), companyWithId(UUID.randomUUID()));
        User companyless = userIn(UUID.randomUUID(), null);

        when(companyAccessService.isNotPartOfCompany(managerId, companyId)).thenReturn(false);
        when(userService.getUserById(foreign.getUserId())).thenReturn(foreign);
        when(userService.getUserById(companyless.getUserId())).thenReturn(companyless);

        assertThatThrownBy(() -> companyService.assertCanReadCompanyUser(managerId, companyId, foreign.getUserId()))
                .isInstanceOf(ResourceNotFoundException.class);
        assertThatThrownBy(() -> companyService.assertCanReadCompanyUser(managerId, companyId, companyless.getUserId()))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void getCompanyUserWithInformation_mapsUserToDto() {
        UUID companyId = UUID.randomUUID();
        Company company = companyWithId(companyId);
        company.setName("Acme");
        User target = userIn(UUID.randomUUID(), company);

        when(userService.getUserById(target.getUserId())).thenReturn(target);

        UserWithTimeParamsDTO dto = companyService.getCompanyUserWithInformation(companyId, target.getUserId());

        assertThat(dto.userId()).isEqualTo(target.getUserId());
        assertThat(dto.email()).isEqualTo(target.getEmail());
        assertThat(dto.companyName()).isEqualTo("Acme");
    }

    // ---------------------------------------------------------------------
    // setCompany()  (M1)
    // ---------------------------------------------------------------------

    @Test
    void setCompany_companyNameOfAnotherCompany_throwsAuthorizationDenied() {
        UUID managerCompanyId = UUID.randomUUID();
        UUID managerId = UUID.randomUUID();
        Company other = companyWithId(UUID.randomUUID());
        User target = userIn(UUID.randomUUID(), null);
        SetCompanyDTO request = new SetCompanyDTO("Other", target.getUserId());

        when(companyAccessService.isNotPartOfCompany(managerId, managerCompanyId)).thenReturn(false);
        when(stringUtils.normalizeString("Other")).thenReturn("Other");
        when(companyRepository.findByName("Other")).thenReturn(Optional.of(other));

        assertThatThrownBy(() -> companyService.setCompany(request, managerCompanyId, managerId))
                .isInstanceOf(AuthorizationDeniedException.class);
        assertThat(target.getCompany()).isNull();
        verify(userService, never()).updateUser(any());
    }

    @Test
    void setCompany_ownCompany_enrollsCompanylessUser() {
        UUID managerCompanyId = UUID.randomUUID();
        UUID managerId = UUID.randomUUID();
        Company own = companyWithId(managerCompanyId);
        User target = userIn(UUID.randomUUID(), null);
        SetCompanyDTO request = new SetCompanyDTO("Own", target.getUserId());

        when(companyAccessService.isNotPartOfCompany(managerId, managerCompanyId)).thenReturn(false);
        when(stringUtils.normalizeString("Own")).thenReturn("Own");
        when(companyRepository.findByName("Own")).thenReturn(Optional.of(own));
        when(userService.getUserById(target.getUserId())).thenReturn(target);

        companyService.setCompany(request, managerCompanyId, managerId);

        assertThat(target.getCompany()).isSameAs(own);
        verify(userService).updateUser(target);
    }

    @Test
    void deleteCompany_softDeletesTheCompanyWithManagerAsActor() {
        UUID companyId = UUID.randomUUID();
        UUID managerId = UUID.randomUUID();

        Company company = new Company();
        ReflectionTestUtils.setField(company, "companyId", companyId);

        User manager = new User("manager@example.com", "Manager", "Rossi", "encoded-pwd");
        ReflectionTestUtils.setField(manager, "userId", managerId);

        when(companyAccessService.getCompanyById(companyId)).thenReturn(company);
        when(companyAccessService.isNotPartOfCompany(managerId, companyId)).thenReturn(false);
        when(userService.getUserById(managerId)).thenReturn(manager);
        when(companyRepository.save(company)).thenReturn(company);

        companyService.deleteCompany(companyId, managerId);

        assertThat(company.isActive()).isFalse();
        assertThat(company.getDeletedBy()).isSameAs(manager);
        assertThat(company.getDeletedAt()).isNotNull();
        verify(companyAccessService).removeCompanyFromUsers(companyId);

        org.mockito.InOrder order = org.mockito.Mockito.inOrder(companyRepository);
        order.verify(companyRepository).save(company);
        order.verify(companyRepository).delete(company);
    }

    @Test
    void deleteCompany_softDeletesTheCompanyRolesWithManagerAsActor() {
        UUID companyId = UUID.randomUUID();
        UUID managerId = UUID.randomUUID();

        Company company = new Company();
        ReflectionTestUtils.setField(company, "companyId", companyId);

        User manager = new User("manager@example.com", "Manager", "Rossi", "encoded-pwd");
        ReflectionTestUtils.setField(manager, "userId", managerId);

        Role role = new Role("ROLE_CASHIER", company);

        when(companyAccessService.getCompanyById(companyId)).thenReturn(company);
        when(companyAccessService.isNotPartOfCompany(managerId, companyId)).thenReturn(false);
        when(userService.getUserById(managerId)).thenReturn(manager);
        when(roleRepository.findAllByCompany_CompanyId(companyId)).thenReturn(List.of(role));
        when(roleRepository.save(role)).thenReturn(role);
        when(companyRepository.save(company)).thenReturn(company);

        companyService.deleteCompany(companyId, managerId);

        // roles are deleted (not blocking the deletion) once every user has been reset to the base role
        assertThat(role.isActive()).isFalse();
        assertThat(role.getDeletedBy()).isSameAs(manager);
        assertThat(role.getDeletedAt()).isNotNull();

        org.mockito.InOrder order = org.mockito.Mockito.inOrder(companyAccessService, roleRepository, companyRepository);
        order.verify(companyAccessService).removeCompanyFromUsers(companyId);
        order.verify(roleRepository).save(role);
        order.verify(roleRepository).delete(role);
        order.verify(companyRepository).save(company);
        order.verify(companyRepository).delete(company);
    }
}
