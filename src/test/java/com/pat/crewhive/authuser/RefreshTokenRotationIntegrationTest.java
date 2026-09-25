package com.pat.crewhive.authuser;

import com.jayway.jsonpath.JsonPath;
import com.pat.crewhive.support.AbstractIntegrationTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import tools.jackson.databind.ObjectMapper;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * End-to-end integration tests for the H5 fix: refresh tokens are stored hashed, rotated within a
 * family, and the reuse of an already rotated token revokes the whole family.
 * <p>
 * {@code /api/auth/rotate} is called without authentication on purpose: it is {@code permitAll}
 * and this is exactly how real clients call it.
 */
class RefreshTokenRotationIntegrationTest extends AbstractIntegrationTest {

    private static final String EMAIL = "rotation@example.test";
    private static final String PASSWORD = "Str0ng!Passw0rd";

    @Autowired
    private JdbcTemplate jdbc;
    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() throws Exception {
        // See EventAuthorizationIntegrationTest: the real registration flow creates no UserPreferences.
        jdbc.execute("ALTER TABLE users ALTER COLUMN user_user_id DROP NOT NULL");
        clean();

        mockMvc.perform(post("/api/auth/register").contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "email", EMAIL, "firstName", "Mario", "lastName", "Rossi", "password", PASSWORD))))
                .andExpect(status().isCreated());
    }

    @AfterEach
    void tearDown() {
        // refresh_token references users: other integration classes delete users without knowing about it.
        clean();
    }

    private void clean() {
        jdbc.execute("DELETE FROM refresh_token");
        jdbc.execute("DELETE FROM shift_template");
        jdbc.execute("DELETE FROM event_users");
        jdbc.execute("DELETE FROM event");
        jdbc.execute("DELETE FROM user_role");
        jdbc.execute("DELETE FROM users");
        jdbc.execute("DELETE FROM company");
    }

    private String login() throws Exception {
        String body = mockMvc.perform(post("/api/auth/login").contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("email", EMAIL, "password", PASSWORD))))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(body, "$.refreshToken");
    }

    private String rotateOk(String refreshToken) throws Exception {
        String body = mockMvc.perform(post("/api/auth/rotate").contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("refreshToken", refreshToken))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(body, "$.refreshToken");
    }

    private void rotateRejected(String refreshToken) throws Exception {
        mockMvc.perform(post("/api/auth/rotate").contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("refreshToken", refreshToken))))
                .andExpect(status().isUnauthorized());
    }

    private int storedTokens() {
        return jdbc.queryForObject("SELECT count(*) FROM refresh_token", Integer.class);
    }

    @Test
    void rotate_withoutAuthentication_returnsNewDifferentTokens() throws Exception {
        String first = login();

        String second = rotateOk(first);

        assertThat(second).isNotEqualTo(first);
        // the rotated token is kept (marked as used) to be able to detect its reuse
        assertThat(storedTokens()).isEqualTo(2);
    }

    @Test
    void rotate_chain_keepsWorking_whenEachNewTokenIsUsed() throws Exception {
        String t1 = login();
        String t2 = rotateOk(t1);
        String t3 = rotateOk(t2);

        assertThat(List.of(t1, t2, t3)).doesNotHaveDuplicates();
        assertThat(jdbc.queryForObject("SELECT count(DISTINCT family_id) FROM refresh_token", Integer.class)).isEqualTo(1);
    }

    @Test
    void reuseOfARotatedToken_isRejected_andRevokesTheWholeFamily() throws Exception {
        String stolen = login();
        String legitimateNext = rotateOk(stolen);

        // the copy of the old token is replayed
        rotateRejected(stolen);

        // the revocation must have been committed (not rolled back with the exception)...
        assertThat(storedTokens()).isZero();
        // ...so the legitimate token of the same family is dead as well
        rotateRejected(legitimateNext);
    }

    @Test
    void unknownToken_isRejected_withoutTouchingExistingSessions() throws Exception {
        String valid = login();

        rotateRejected(java.util.UUID.randomUUID().toString());

        assertThat(storedTokens()).isEqualTo(1);
        rotateOk(valid);
    }

    @Test
    void expiredToken_isRejected_evenWhenExpiredByASingleSecond() throws Exception {
        String token = login();
        jdbc.update("UPDATE refresh_token SET expires_at = now() - interval '1 second'");

        rotateRejected(token);
    }

    @Test
    void login_replacesThePreviousSession() throws Exception {
        String old = login();
        String fresh = login();

        assertThat(storedTokens()).isEqualTo(1);
        rotateRejected(old);
        // the replay above hits an unknown token (deleted by the new login): the new session survives
        rotateOk(fresh);
    }

    @Test
    void refreshTokenIsNeverStoredInClear() throws Exception {
        String token = login();

        List<String> hashes = jdbc.queryForList("SELECT token_hash FROM refresh_token", String.class);

        assertThat(hashes).hasSize(1);
        assertThat(hashes.getFirst()).isNotEqualTo(token).hasSize(64).matches("[0-9a-f]{64}");
        assertThat(jdbc.queryForObject(
                "SELECT count(*) FROM refresh_token WHERE token_hash = ?", Integer.class, token)).isZero();
    }
}
