package com.pat.crewhive.manager;

import com.pat.crewhive.common.StringUtils;
import com.pat.crewhive.company.Company;
import com.pat.crewhive.company.CompanyService;
import com.pat.crewhive.security.exception.custom.ResourceNotFoundException;
import com.pat.crewhive.user.User;
import com.pat.crewhive.user.UserService;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link RoleService#updateUserRole} (H3) and {@link UpdateUserRoleDTO} validation.
 */
@ExtendWith(MockitoExtension.class)
class RoleServiceTest {

    @Mock
    private RoleRepository roleRepository;
    @Mock
    private UserService userService;
    @Mock
    private CompanyService companyService;
    @Mock
    private StringUtils stringUtils;
    @Mock
    private RoleAssignmentService roleAssignmentService;

    private RoleService roleService;

    @BeforeEach
    void setUp() {
        roleService = new RoleService(roleRepository, userService, companyService, stringUtils, roleAssignmentService);
    }

    private Company company(UUID companyId) {
        Company c = new Company();
        ReflectionTestUtils.setField(c, "companyId", companyId);
        return c;
    }

    private User user(Company company) {
        User u = new User("u@example.com", "Luigi", "Verdi", "encoded-pwd");
        ReflectionTestUtils.setField(u, "userId", UUID.randomUUID());
        u.setCompany(company);
        return u;
    }

    private void stubRole(Company company, Role role) {
        when(stringUtils.normalizeRole("cashier")).thenReturn("ROLE_CASHIER");
        when(companyService.getCompanyById(company.getCompanyId())).thenReturn(company);
        when(roleRepository.findByRoleNameIgnoreCaseAndCompany("ROLE_CASHIER", company)).thenReturn(Optional.of(role));
    }

    @Test
    void updateUserRole_targetInSameCompany_assignsRole() {
        Company company = company(UUID.randomUUID());
        Role role = new Role("ROLE_CASHIER", company);
        User target = user(company);
        stubRole(company, role);
        when(userService.getUserById(target.getUserId())).thenReturn(target);

        roleService.updateUserRole(target.getUserId(), "cashier", company.getCompanyId());

        assertThat(target.getRoles()).anyMatch(ur -> ur.getRole() == role);
    }

    @Test
    void updateUserRole_targetInOtherCompanyOrNone_throwsResourceNotFoundAndAssignsNothing() {
        Company mine = company(UUID.randomUUID());
        Role role = new Role("ROLE_CASHIER", mine);
        User foreign = user(company(UUID.randomUUID()));
        User companyless = user(null);
        stubRole(mine, role);
        when(userService.getUserById(foreign.getUserId())).thenReturn(foreign);
        when(userService.getUserById(companyless.getUserId())).thenReturn(companyless);

        assertThatThrownBy(() -> roleService.updateUserRole(foreign.getUserId(), "cashier", mine.getCompanyId()))
                .isInstanceOf(ResourceNotFoundException.class);
        assertThatThrownBy(() -> roleService.updateUserRole(companyless.getUserId(), "cashier", mine.getCompanyId()))
                .isInstanceOf(ResourceNotFoundException.class);

        assertThat(foreign.getRoles()).noneMatch(ur -> ur.getRole() == role);
        assertThat(companyless.getRoles()).noneMatch(ur -> ur.getRole() == role);
    }

    @Test
    void updateUserRole_roleNotFound_throwsResourceNotFound() {
        Company company = company(UUID.randomUUID());
        when(stringUtils.normalizeRole("cashier")).thenReturn("ROLE_CASHIER");
        when(companyService.getCompanyById(company.getCompanyId())).thenReturn(company);
        when(roleRepository.findByRoleNameIgnoreCaseAndCompany("ROLE_CASHIER", company)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> roleService.updateUserRole(UUID.randomUUID(), "cashier", company.getCompanyId()))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ---------------------------------------------------------------------
    // createRole: nomi riservati (H6)
    // ---------------------------------------------------------------------

    @ParameterizedTest
    @ValueSource(strings = {"user", "dev", "manager", "ROLE_DEV", "ROLE_MANAGER", "ROLE_USER"})
    void createRole_reservedName_throwsIllegalArgumentAndSavesNothing(String name) {
        // StringUtils è un mock: replico normalizeRole
        String normalized = name.toUpperCase().startsWith("ROLE_") ? name.toUpperCase() : "ROLE_" + name.toUpperCase();
        when(stringUtils.normalizeRole(name)).thenReturn(normalized);

        assertThatThrownBy(() -> roleService.createRole(name, UUID.randomUUID()))
                .isInstanceOf(IllegalArgumentException.class);

        verify(roleRepository, never()).save(any());
        verifyNoInteractions(companyService);
    }

    @Test
    void createRole_regularName_savesCompanyRole() {
        Company company = company(UUID.randomUUID());
        when(stringUtils.normalizeRole("cashier")).thenReturn("ROLE_CASHIER");
        when(companyService.getCompanyById(company.getCompanyId())).thenReturn(company);
        when(roleRepository.existsByRoleNameIgnoreCaseAndCompany("ROLE_CASHIER", company)).thenReturn(false);

        roleService.createRole("cashier", company.getCompanyId());

        verify(roleRepository).save(argThat(r -> r.getRoleName().equals("ROLE_CASHIER") && r.getCompany() == company));
    }

    // ---------------------------------------------------------------------
    // UpdateUserRoleDTO validation: used to blow up with UnexpectedTypeException (500 on every call)
    // ---------------------------------------------------------------------

    @Test
    void updateUserRoleDto_validPayload_hasNoViolations() {
        Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

        assertThat(validator.validate(new UpdateUserRoleDTO("cashier", UUID.randomUUID()))).isEmpty();
    }

    @Test
    void updateUserRoleDto_blankRoleAndNullUser_areReportedAsViolations() {
        Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

        assertThat(validator.validate(new UpdateUserRoleDTO(" ", null)))
                .extracting(v -> v.getPropertyPath().toString())
                .contains("newRole", "userId");
    }
}
