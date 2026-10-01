package com.pat.crewhive.company;

import com.pat.crewhive.user.User;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link Company}, {@link AddressJSON} and {@link CompanyType}.
 */
class CompanyEntityTest {

    private static final AddressJSON ADDRESS = new AddressJSON("Via Roma 1", "Milano", "20100", "MI", "Italia");

    @Test
    void registrationConstructor_copiesNameAddressAndType() {
        Company company = new Company(new CompanyRegistrationDTO("acme", CompanyType.BAR, ADDRESS));

        assertThat(company.getName()).isEqualTo("acme");
        assertThat(company.getAddressJSON()).isEqualTo(ADDRESS);
        assertThat(company.getCompanyType()).isEqualTo(CompanyType.BAR);
        assertThat(company.getCompanyId()).isNull();
        assertThat(company.getUsers()).isEmpty();
    }

    @Test
    void fullConstructor_setsEveryField() {
        UUID id = UUID.randomUUID();
        Set<User> users = new LinkedHashSet<>();

        Company company = new Company(id, "acme", ADDRESS, users, CompanyType.HOSPITAL);

        assertThat(company.getCompanyId()).isEqualTo(id);
        assertThat(company.getName()).isEqualTo("acme");
        assertThat(company.getAddressJSON()).isSameAs(ADDRESS);
        assertThat(company.getUsers()).isSameAs(users);
        assertThat(company.getCompanyType()).isEqualTo(CompanyType.HOSPITAL);
    }

    @Test
    void settersReplaceTheValues() {
        Company company = new Company();
        Set<User> users = new LinkedHashSet<>();

        company.setName("beta");
        company.setAddressJSON(ADDRESS);
        company.setUsers(users);
        company.setCompanyType(CompanyType.OTHER);

        assertThat(company.getName()).isEqualTo("beta");
        assertThat(company.getAddressJSON()).isSameAs(ADDRESS);
        assertThat(company.getUsers()).isSameAs(users);
        assertThat(company.getCompanyType()).isEqualTo(CompanyType.OTHER);
    }

    @Test
    void companyTypeLabels() {
        assertThat(CompanyType.HOSPITAL.getLabel()).isEqualTo("Hospital");
        assertThat(CompanyType.RESTAURANT.getLabel()).isEqualTo("Restaurant");
        assertThat(CompanyType.BAR.getLabel()).isEqualTo("Bar");
        assertThat(CompanyType.OTHER.getLabel()).isEqualTo("Other");
    }
}
