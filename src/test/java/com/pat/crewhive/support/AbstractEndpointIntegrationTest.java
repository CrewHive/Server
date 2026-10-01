package com.pat.crewhive.support;

import com.pat.crewhive.company.Company;
import com.pat.crewhive.company.CompanyRepository;
import com.pat.crewhive.company.CompanyType;
import com.pat.crewhive.security.CustomUserDetails;
import com.pat.crewhive.user.User;
import com.pat.crewhive.user.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import tools.jackson.databind.ObjectMapper;

import java.util.Date;
import java.util.Set;
import java.util.UUID;

/**
 * Base class for the endpoint tests: starts and leaves the database empty (the integration test classes
 * share one Postgres), and offers helpers to build tenants and to authenticate as one of their users.
 * Requests are authenticated with {@link #as(CustomUserDetails)}, which bypasses the JWT filter: the
 * filter itself is covered by its own tests.
 */
public abstract class AbstractEndpointIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    protected CompanyRepository companyRepository;
    @Autowired
    protected UserRepository userRepository;
    @Autowired
    protected JdbcTemplate jdbc;
    @Autowired
    protected ObjectMapper objectMapper;

    @BeforeEach
    void resetDatabaseBeforeTest() {
        // Mirrors the real registration flow, which creates no UserPreferences (see EventAuthorizationIntegrationTest).
        jdbc.execute("ALTER TABLE users ALTER COLUMN user_user_id DROP NOT NULL");
        jdbc.execute("INSERT INTO event_type (id, name, active) VALUES "
                + "(1, 'PUBLIC', true), (2, 'PRIVATE', true) ON CONFLICT (id) DO NOTHING");
        clearTenantData();
    }

    @AfterEach
    void resetDatabaseAfterTest() {
        clearTenantData();
    }

    private void clearTenantData() {
        jdbc.execute("DELETE FROM refresh_token");
        jdbc.execute("DELETE FROM shift_user");
        jdbc.execute("DELETE FROM shift_worked");
        jdbc.execute("DELETE FROM shift_programmed");
        jdbc.execute("DELETE FROM shift_template");
        jdbc.execute("DELETE FROM event_users");
        jdbc.execute("DELETE FROM event");
        jdbc.execute("DELETE FROM user_role");
        jdbc.execute("DELETE FROM users");
        jdbc.execute("DELETE FROM role WHERE company_id IS NOT NULL");
        jdbc.execute("DELETE FROM company");
    }

    protected Company newCompany(String name) {
        Company company = new Company();
        company.setName(name);
        company.setCompanyType(CompanyType.OTHER);
        return companyRepository.saveAndFlush(company);
    }

    protected User newUser(String email, Company company) {
        User user = new User(email, "First", "Last", "hashed-pwd");
        user.setCompany(company);
        user.setWorking(true);
        return userRepository.saveAndFlush(user);
    }

    protected CustomUserDetails asUser(User user) {
        return principalOf(user, "ROLE_USER");
    }

    protected CustomUserDetails asManager(User user) {
        return principalOf(user, "ROLE_USER", "ROLE_MANAGER");
    }

    /** Like the principal the JWT filter builds from the claims: it carries the real email of the user. */
    private CustomUserDetails principalOf(User user, String... roles) {
        return new CustomUserDetails(
                user.getUserId(),
                user.getEmail(),
                user.getFirstName(),
                user.getLastName(),
                Set.of(roles),
                user.getCompany() == null ? null : user.getCompany().getCompanyId(),
                true,
                "test-jti-" + UUID.randomUUID(),
                new Date(System.currentTimeMillis() + 3_600_000));
    }

    protected String json(Object value) throws Exception {
        return objectMapper.writeValueAsString(value);
    }
}
