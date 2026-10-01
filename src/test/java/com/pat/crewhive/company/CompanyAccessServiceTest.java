package com.pat.crewhive.company;

import com.pat.crewhive.manager.RoleAssignmentService;
import com.pat.crewhive.security.TokenBlackListService;
import com.pat.crewhive.user.User;
import com.pat.crewhive.user.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link CompanyAccessService#removeCompanyFromUsers} (M5).
 */
@ExtendWith(MockitoExtension.class)
class CompanyAccessServiceTest {

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

    private User user(Company company) {
        User u = new User(UUID.randomUUID() + "@example.com", "Mario", "Rossi", "encoded-pwd");
        ReflectionTestUtils.setField(u, "userId", UUID.randomUUID());
        u.setCompany(company);
        return u;
    }

    @Test
    void removeCompanyFromUsers_detachesResetsRolesAndRevokesTokensOfEveryUser() {
        UUID companyId = UUID.randomUUID();
        Company company = new Company();
        ReflectionTestUtils.setField(company, "companyId", companyId);
        User first = user(company);
        User second = user(company);
        when(userService.getAllUsersInCompany(companyId)).thenReturn(List.of(first, second));

        companyAccessService.removeCompanyFromUsers(companyId);

        assertThat(first.getCompany()).isNull();
        assertThat(second.getCompany()).isNull();
        verify(roleAssignmentService).resetToBaseRole(first);
        verify(roleAssignmentService).resetToBaseRole(second);
        verify(tokenBlackListService).revokeAllForUser(first.getUserId());
        verify(tokenBlackListService).revokeAllForUser(second.getUserId());
    }
}
