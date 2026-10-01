package com.pat.crewhive.manager;

import com.pat.crewhive.company.Company;
import com.pat.crewhive.user.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link RoleAssignmentService}.
 */
@ExtendWith(MockitoExtension.class)
class RoleAssignmentServiceTest {

    @Mock
    private RoleRepository roleRepository;

    private RoleAssignmentService roleAssignmentService;

    @BeforeEach
    void setUp() {
        roleAssignmentService = new RoleAssignmentService(roleRepository);
    }

    private User user() {
        User u = new User("mario.rossi@example.com", "Mario", "Rossi", "encoded-pwd");
        ReflectionTestUtils.setField(u, "userId", UUID.randomUUID());
        return u;
    }

    private Company company() {
        Company c = new Company();
        ReflectionTestUtils.setField(c, "companyId", UUID.randomUUID());
        return c;
    }

    // ---------------------------------------------------------------------
    // getOrCreateGlobalRoleUser()
    // ---------------------------------------------------------------------

    @Test
    void getOrCreateGlobalRoleUser_returnsTheExistingGlobalRole() {
        Role existing = new Role("ROLE_USER", null);
        when(roleRepository.findByRoleNameIgnoreCaseAndCompanyIsNull("ROLE_USER")).thenReturn(Optional.of(existing));

        assertThat(roleAssignmentService.getOrCreateGlobalRoleUser()).isSameAs(existing);
        verify(roleRepository, never()).save(any());
    }

    @Test
    void getOrCreateGlobalRoleUser_createsAGlobalRoleWhenMissing() {
        when(roleRepository.findByRoleNameIgnoreCaseAndCompanyIsNull("ROLE_USER")).thenReturn(Optional.empty());
        when(roleRepository.save(any(Role.class))).thenAnswer(inv -> inv.getArgument(0));

        Role created = roleAssignmentService.getOrCreateGlobalRoleUser();

        assertThat(created.getRoleName()).isEqualTo("ROLE_USER");
        assertThat(created.getCompany()).isNull();
        verify(roleRepository).save(created);
    }

    // ---------------------------------------------------------------------
    // resetToBaseRole()
    // ---------------------------------------------------------------------

    @Test
    void resetToBaseRole_removesEveryRoleButTheBaseOne() {
        Role base = new Role("ROLE_USER", null);
        Role manager = new Role("ROLE_MANAGER", company());
        Role cashier = new Role("ROLE_CASHIER", company());
        when(roleRepository.findByRoleNameIgnoreCaseAndCompanyIsNull("ROLE_USER")).thenReturn(Optional.of(base));
        User user = user();
        user.addRole(base);
        user.addRole(manager);
        user.addRole(cashier);

        roleAssignmentService.resetToBaseRole(user);

        assertThat(user.getRoles()).extracting(UserRole::getRole).containsExactly(base);
        assertThat(manager.getUsers()).isEmpty();
        assertThat(cashier.getUsers()).isEmpty();
    }

    @Test
    void resetToBaseRole_addsTheBaseRoleWhenTheUserDidNotHaveIt() {
        Role base = new Role("ROLE_USER", null);
        Role manager = new Role("ROLE_MANAGER", company());
        when(roleRepository.findByRoleNameIgnoreCaseAndCompanyIsNull("ROLE_USER")).thenReturn(Optional.of(base));
        User user = user();
        user.addRole(manager);

        roleAssignmentService.resetToBaseRole(user);

        assertThat(user.getRoles()).extracting(UserRole::getRole).containsExactly(base);
    }

    @Test
    void resetToBaseRole_isIdempotent() {
        Role base = new Role("ROLE_USER", null);
        when(roleRepository.findByRoleNameIgnoreCaseAndCompanyIsNull("ROLE_USER")).thenReturn(Optional.of(base));
        User user = user();
        user.addRole(base);

        roleAssignmentService.resetToBaseRole(user);
        roleAssignmentService.resetToBaseRole(user);

        assertThat(user.getRoles()).hasSize(1);
        assertThat(user.getRoles()).extracting(UserRole::getRole).containsExactly(base);
    }

    @Test
    void resetToBaseRole_onUserWithoutRoles_givesOnlyTheBaseRole() {
        Role base = new Role("ROLE_USER", null);
        when(roleRepository.findByRoleNameIgnoreCaseAndCompanyIsNull("ROLE_USER")).thenReturn(Optional.of(base));
        User user = user();

        roleAssignmentService.resetToBaseRole(user);

        assertThat(user.getRoles()).extracting(UserRole::getRole).containsExactly(base);
    }
}
