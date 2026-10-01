package com.pat.crewhive.manager;

import com.pat.crewhive.company.Company;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashSet;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link Role}.
 */
class RoleTest {

    @Test
    void globalConstructor_hasNoCompany() {
        Role role = new Role("ROLE_USER");

        assertThat(role.getRoleName()).isEqualTo("ROLE_USER");
        assertThat(role.getCompany()).isNull();
        assertThat(role.getUsers()).isEmpty();
        assertThat(role.getRoleId()).isNull();
    }

    @Test
    void companyConstructor_scopesTheRoleToTheCompany() {
        Company company = new Company();

        Role role = new Role("ROLE_CASHIER", company);

        assertThat(role.getCompany()).isSameAs(company);
    }

    @Test
    void settersReplaceTheValues() {
        Role role = new Role();
        Company company = new Company();
        LinkedHashSet<UserRole> users = new LinkedHashSet<>();

        role.setRoleId(5L);
        role.setRoleName("ROLE_X");
        role.setCompany(company);
        role.setUsers(users);

        assertThat(role.getRoleId()).isEqualTo(5L);
        assertThat(role.getRoleName()).isEqualTo("ROLE_X");
        assertThat(role.getCompany()).isSameAs(company);
        assertThat(role.getUsers()).isSameAs(users);
    }

    @Test
    void reservedRoleNameConstants() {
        assertThat(Role.ROLE_USER).isEqualTo("ROLE_USER");
        assertThat(Role.ROLE_MANAGER).isEqualTo("ROLE_MANAGER");
        assertThat(Role.ROLE_DEV).isEqualTo("ROLE_DEV");
    }
}
