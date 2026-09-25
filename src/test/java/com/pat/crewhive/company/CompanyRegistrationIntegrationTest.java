package com.pat.crewhive.company;

import com.pat.crewhive.security.JwtService;
import com.pat.crewhive.support.AbstractIntegrationTest;
import com.pat.crewhive.user.User;
import com.pat.crewhive.user.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MvcResult;
import tools.jackson.databind.ObjectMapper;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * End-to-end integration tests for the H6 fix: {@code POST /company/register} crea un ROLE_MANAGER
 * <em>per-company</em> (mai il ruolo globale) e non si può ripetere se l'utente ha già una company.
 */
class CompanyRegistrationIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private UserRepository userRepository;
    @Autowired
    private JwtService jwtService;
    @Autowired
    private JdbcTemplate jdbc;
    @Autowired
    private ObjectMapper objectMapper;

    private User user;

    @BeforeEach
    void setUp() {
        // See EventAuthorizationIntegrationTest: mirrors the real registration flow (no UserPreferences).
        jdbc.execute("ALTER TABLE users ALTER COLUMN user_user_id DROP NOT NULL");

        // Targeted deletes in FK order (leftovers of other integration test classes included).
        jdbc.execute("DELETE FROM refresh_token");
        jdbc.execute("DELETE FROM shift_template");
        jdbc.execute("DELETE FROM event_users");
        jdbc.execute("DELETE FROM event");
        jdbc.execute("DELETE FROM user_role");
        jdbc.execute("DELETE FROM users");
        jdbc.execute("DELETE FROM role WHERE company_id IS NOT NULL");
        jdbc.execute("DELETE FROM company");

        User u = new User("founder@example.test", "First", "Last", "hashed-pwd");
        u.setWorking(true);
        user = userRepository.saveAndFlush(u);
    }

    @AfterEach
    void cleanUp() {
        // Le altre classi di integration test cancellano users/company senza conoscere refresh_token
        // e i ruoli company-scoped creati qui: lascio il DB pulito.
        jdbc.execute("DELETE FROM refresh_token");
        jdbc.execute("DELETE FROM user_role");
        jdbc.execute("DELETE FROM users");
        jdbc.execute("DELETE FROM role WHERE company_id IS NOT NULL");
        jdbc.execute("DELETE FROM company");
    }

    @Test
    void register_createsCompanyScopedManagerRole_andTokenCarriesIt() throws Exception {
        MvcResult result = mockMvc.perform(post("/company/register")
                        .with(as(principal(user.getUserId(), null, "ROLE_USER")))
                        .contentType(APPLICATION_JSON)
                        .content(body("Acme Srl")))
                .andExpect(status().isOk())
                .andReturn();

        String accessToken = objectMapper.readTree(result.getResponse().getContentAsString())
                .get("accessToken").asString();
        var claims = jwtService.validateToken(accessToken);
        assertThat(claims.get("role", String.class)).contains("ROLE_MANAGER");
        assertThat(claims.get("companyId", String.class)).isNotNull();

        Integer scoped = jdbc.queryForObject(
                "SELECT count(*) FROM role WHERE role_name = 'ROLE_MANAGER' AND company_id = ?::uuid",
                Integer.class, claims.get("companyId", String.class));
        Integer global = jdbc.queryForObject(
                "SELECT count(*) FROM role WHERE role_name = 'ROLE_MANAGER' AND company_id IS NULL", Integer.class);
        assertThat(scoped).isEqualTo(1);
        assertThat(global).isZero();
    }

    @Test
    void register_whenUserAlreadyInCompany_isConflict() throws Exception {
        mockMvc.perform(post("/company/register")
                        .with(as(principal(user.getUserId(), null, "ROLE_USER")))
                        .contentType(APPLICATION_JSON)
                        .content(body("Acme Srl")))
                .andExpect(status().isOk());

        mockMvc.perform(post("/company/register")
                        .with(as(principal(user.getUserId(), null, "ROLE_USER")))
                        .contentType(APPLICATION_JSON)
                        .content(body("Second Srl")))
                .andExpect(status().isConflict());

        Integer companies = jdbc.queryForObject("SELECT count(*) FROM company", Integer.class);
        assertThat(companies).isEqualTo(1);
    }

    private String body(String name) throws Exception {
        return objectMapper.writeValueAsString(Map.of("companyName", name, "companyType", "OTHER"));
    }
}
