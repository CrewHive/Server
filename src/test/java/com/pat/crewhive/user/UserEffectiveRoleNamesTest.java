package com.pat.crewhive.user;

import com.pat.crewhive.company.Company;
import com.pat.crewhive.manager.Role;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link User#getEffectiveRoleNames()} (H6): il ROLE_MANAGER vale solo
 * per la company dell'utente, mai come ruolo globale.
 */
class UserEffectiveRoleNamesTest {

    private Company company() {
        Company c = new Company();
        ReflectionTestUtils.setField(c, "companyId", UUID.randomUUID());
        return c;
    }

    private User user(Company company) {
        User u = new User("u@example.com", "Luigi", "Verdi", "encoded-pwd");
        u.setCompany(company);
        return u;
    }

    @Test
    void globalUserRole_isIncluded() {
        User u = user(null);
        u.addRole(new Role(Role.ROLE_USER, null));

        assertThat(u.getEffectiveRoleNames()).containsExactly(Role.ROLE_USER);
    }

    @Test
    void legacyGlobalManagerRole_isExcluded() {
        User u = user(company());
        u.addRole(new Role(Role.ROLE_USER, null));
        u.addRole(new Role(Role.ROLE_MANAGER, null));

        assertThat(u.getEffectiveRoleNames()).containsExactly(Role.ROLE_USER);
    }

    @Test
    void managerRoleOfOwnCompany_isIncluded() {
        Company own = company();
        User u = user(own);
        u.addRole(new Role(Role.ROLE_USER, null));
        u.addRole(new Role(Role.ROLE_MANAGER, own));

        assertThat(u.getEffectiveRoleNames()).containsExactlyInAnyOrder(Role.ROLE_USER, Role.ROLE_MANAGER);
    }

    @Test
    void managerRoleOfOtherCompany_isExcluded() {
        User u = user(company());
        u.addRole(new Role(Role.ROLE_USER, null));
        u.addRole(new Role(Role.ROLE_MANAGER, company()));

        assertThat(u.getEffectiveRoleNames()).containsExactly(Role.ROLE_USER);
    }

    @Test
    void companyRole_withoutCurrentCompany_isExcluded() {
        User u = user(null);
        u.addRole(new Role(Role.ROLE_USER, null));
        u.addRole(new Role(Role.ROLE_MANAGER, company()));

        assertThat(u.getEffectiveRoleNames()).containsExactly(Role.ROLE_USER);
    }

    @Test
    void companyRole_isMatchedByIdNotByInstance() {
        Company own = company();
        Company sameIdOtherInstance = new Company();
        ReflectionTestUtils.setField(sameIdOtherInstance, "companyId", own.getCompanyId());
        User u = user(own);
        u.addRole(new Role("ROLE_CASHIER", sameIdOtherInstance));

        assertThat(u.getEffectiveRoleNames()).containsExactly("ROLE_CASHIER");
    }
}
