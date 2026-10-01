package com.pat.crewhive.authuser;

import com.pat.crewhive.support.AbstractIntegrationTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.ResultActions;
import tools.jackson.databind.ObjectMapper;

import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integration tests for M3: {@code /register} answers the same way whether the email is new or
 * already registered, the account is created only by {@code /verify-email}, and {@code /login}
 * does not tell "unknown email" apart from "wrong password".
 */
class RegistrationEmailIntegrationTest extends AbstractIntegrationTest {

    private static final String EMAIL = "verify@example.test";
    private static final String PASSWORD = "Str0ng!Passw0rd";

    @Autowired
    private JdbcTemplate jdbc;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private StringRedisTemplate redis;

    @BeforeEach
    void setUp() {
        // See RefreshTokenRotationIntegrationTest: the real registration flow creates no UserPreferences.
        jdbc.execute("ALTER TABLE users ALTER COLUMN user_user_id DROP NOT NULL");
        clean();
    }

    @AfterEach
    void tearDown() {
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
        Set<String> keys = redis.keys("pending-registration:*");
        if (keys != null && !keys.isEmpty()) redis.delete(keys);
    }

    private ResultActions register(String email) throws Exception {
        return mockMvc.perform(post("/api/auth/register").contentType(APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of(
                        "email", email, "firstName", "Mario", "lastName", "Rossi", "password", PASSWORD))));
    }

    private ResultActions confirm(String token) throws Exception {
        return mockMvc.perform(post("/api/auth/verify-email").contentType(APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("token", token))));
    }

    private ResultActions login(String email, String password) throws Exception {
        return mockMvc.perform(post("/api/auth/login").contentType(APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("email", email, "password", password))));
    }

    private String registerAndCaptureToken(String email) throws Exception {
        register(email).andExpect(status().isAccepted());
        ArgumentCaptor<String> token = ArgumentCaptor.forClass(String.class);
        verify(mailService).sendVerification(eq(email), token.capture());
        return token.getValue();
    }

    private static String withoutTimestamp(String json) {
        return json.replaceAll("\"timestamp\":\"[^\"]*\",?", "");
    }

    private int usersWithEmail(String email) {
        return jdbc.queryForObject("SELECT count(*) FROM users WHERE email = ?", Integer.class, email);
    }

    @Test
    void register_createsNoUserUntilTheEmailIsConfirmed() throws Exception {
        String token = registerAndCaptureToken(EMAIL);

        assertThat(usersWithEmail(EMAIL)).isZero();
        login(EMAIL, PASSWORD).andExpect(status().isUnauthorized());

        confirm(token).andExpect(status().isCreated());

        assertThat(usersWithEmail(EMAIL)).isEqualTo(1);
        login(EMAIL, PASSWORD).andExpect(status().isOk());
    }

    @Test
    void register_answersTheSame_whenEmailIsAlreadyRegistered() throws Exception {
        confirm(registerAndCaptureToken(EMAIL)).andExpect(status().isCreated());

        String newBody = register("other@example.test").andExpect(status().isAccepted())
                .andReturn().getResponse().getContentAsString();
        String existingBody = register(EMAIL).andExpect(status().isAccepted())
                .andReturn().getResponse().getContentAsString();

        assertThat(existingBody).isEqualTo(newBody);
        verify(mailService).sendAccountAlreadyExists(EMAIL);
        // sendVerification(EMAIL, ...) was called exactly once: by the first registration, not by the second
        verify(mailService, times(1)).sendVerification(eq(EMAIL), anyString());
    }

    @Test
    void verifyEmail_tokenIsSingleUse() throws Exception {
        String token = registerAndCaptureToken(EMAIL);

        confirm(token).andExpect(status().isCreated());
        confirm(token).andExpect(status().isUnauthorized());

        assertThat(usersWithEmail(EMAIL)).isEqualTo(1);
    }

    @Test
    void verifyEmail_rejectsUnknownToken() throws Exception {
        confirm("not-a-real-token").andExpect(status().isUnauthorized());
    }

    @Test
    void pendingRegistration_isStoredUnderTheHashOfTheToken_notTheToken() throws Exception {
        String token = registerAndCaptureToken(EMAIL);

        Set<String> keys = redis.keys("pending-registration:*");

        assertThat(keys).hasSize(1);
        assertThat(keys.iterator().next()).doesNotContain(token);
        assertThat(redis.getExpire(keys.iterator().next())).isPositive();
    }

    @Test
    void login_answersTheSame_forUnknownEmailAndWrongPassword() throws Exception {
        confirm(registerAndCaptureToken(EMAIL)).andExpect(status().isCreated());

        var wrongPassword = login(EMAIL, "Wr0ng!Passw0rd").andExpect(status().isUnauthorized())
                .andReturn().getResponse();
        var unknownEmail = login("nobody@example.test", "Wr0ng!Passw0rd").andExpect(status().isUnauthorized())
                .andReturn().getResponse();

        // the body carries a per-request timestamp: everything else must be identical
        assertThat(withoutTimestamp(unknownEmail.getContentAsString()))
                .isEqualTo(withoutTimestamp(wrongPassword.getContentAsString()));
    }
}
