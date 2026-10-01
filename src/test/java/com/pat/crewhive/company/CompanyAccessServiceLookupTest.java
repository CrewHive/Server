package com.pat.crewhive.company;

import com.pat.crewhive.manager.RoleAssignmentService;
import com.pat.crewhive.security.TokenBlackListService;
import com.pat.crewhive.security.exception.custom.ResourceNotFoundException;
import com.pat.crewhive.user.User;
import com.pat.crewhive.user.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

/**
 * Unit tests for the lookups of {@link CompanyAccessService}: {@code getCompanyById} and {@code isNotPartOfCompany}.
 */
@ExtendWith(MockitoExtension.class)
class CompanyAccessServiceLookupTest {

    @Mock
    private CompanyRepository companyRepository;
    @Mock
    private UserService userService;
    @Mock
    private RoleAssignmentService roleAssignmentService;
    @Mock
    private TokenBlackListService tokenBlackListService;

    private CompanyAccessService companyAccessService;

    @BeforeEach
    void setUp() {
        companyAccessService = new CompanyAccessService(
                companyRepository, userService, roleAssignmentService, tokenBlackListService);
    }

    private Company company(UUID id) {
        Company c = new Company();
        ReflectionTestUtils.setField(c, "companyId", id);
        return c;
    }

    private User user(UUID id, Company company) {
        User u = new User("u@example.com", "Mario", "Rossi", "encoded-pwd");
        ReflectionTestUtils.setField(u, "userId", id);
        u.setCompany(company);
        return u;
    }

    @Test
    void getCompanyById_existing_returnsIt() {
        UUID id = UUID.randomUUID();
        Company company = company(id);
        when(companyRepository.findById(id)).thenReturn(Optional.of(company));

        assertThat(companyAccessService.getCompanyById(id)).isSameAs(company);
    }

    @Test
    void getCompanyById_unknown_throwsResourceNotFound() {
        UUID id = UUID.randomUUID();
        when(companyRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> companyAccessService.getCompanyById(id)).isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void isNotPartOfCompany_userInTheCompany_isFalse() {
        UUID companyId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        when(userService.getUserById(userId)).thenReturn(user(userId, company(companyId)));

        assertThat(companyAccessService.isNotPartOfCompany(userId, companyId)).isFalse();
    }

    @Test
    void isNotPartOfCompany_userInAnotherCompany_isTrue() {
        UUID userId = UUID.randomUUID();
        when(userService.getUserById(userId)).thenReturn(user(userId, company(UUID.randomUUID())));

        assertThat(companyAccessService.isNotPartOfCompany(userId, UUID.randomUUID())).isTrue();
    }

    @Test
    void isNotPartOfCompany_userWithoutCompany_isTrue() {
        UUID userId = UUID.randomUUID();
        when(userService.getUserById(userId)).thenReturn(user(userId, null));

        assertThat(companyAccessService.isNotPartOfCompany(userId, UUID.randomUUID())).isTrue();
    }
}
