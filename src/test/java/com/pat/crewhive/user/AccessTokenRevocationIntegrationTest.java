package com.pat.crewhive.user;

import com.jayway.jsonpath.JsonPath;
import com.pat.crewhive.support.AbstractIntegrationTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import tools.jackson.databind.ObjectMapper;

import java.util.Map;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * End-to-end test for M5: an access token issued before a password change is rejected
 * afterwards, even though it is still valid by signature and expiry.
 */
class AccessTokenRevocationIntegrationTest extends AbstractIntegrationTest {

    private static final String EMAIL = "revocation@example.test";
    private static final String PASSWORD = "Str0ng!Passw0rd";

    @Autowired
    private JdbcTemplate jdbc;
    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() throws Exception {
        jdbc.execute("ALTER TABLE users ALTER COLUMN user_user_id DROP NOT NULL");
        clean();

        mockMvc.perform(post("/api/auth/register").contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "email", EMAIL, "firstName", "Mario", "lastName", "Rossi", "password", PASSWORD))))
                .andExpect(status().isAccepted());

        ArgumentCaptor<String> token = ArgumentCaptor.forClass(String.class);
        verify(mailService).sendVerification(eq(EMAIL), token.capture());

        mockMvc.perform(post("/api/auth/verify-email").contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("token", token.getValue()))))
                .andExpect(status().isCreated());
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
        jdbc.execute("DELETE FROM role WHERE company_id IS NOT NULL");
        jdbc.execute("DELETE FROM company");
    }

    private String loginBearer() throws Exception {
        String loginBody = mockMvc.perform(post("/api/auth/login").contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("email", EMAIL, "password", PASSWORD))))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return "Bearer " + JsonPath.read(loginBody, "$.accessToken");
    }

    @Test
    void accessToken_isRejected_afterPasswordChange() throws Exception {
        String bearer = loginBearer();

        mockMvc.perform(get("/api/user/me").header("Authorization", bearer)).andExpect(status().isOk());

        // iat has second precision and a token issued in the same second as the revocation is
        // accepted by design (see TokenBlackListService#revokeAllForUser).
        Thread.sleep(1100);

        mockMvc.perform(patch("/api/user/update-password").header("Authorization", bearer)
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "oldPassword", PASSWORD, "newPassword", "N3w!Str0ngPassw0rd"))))
                .andExpect(status().is2xxSuccessful());

        mockMvc.perform(get("/api/user/me").header("Authorization", bearer))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void accessToken_isRejected_afterDeleteAccount() throws Exception {
        String bearer = loginBearer();

        mockMvc.perform(get("/api/user/me").header("Authorization", bearer)).andExpect(status().isOk());

        Thread.sleep(1100);

        mockMvc.perform(delete("/api/user/delete-account").header("Authorization", bearer))
                .andExpect(status().is2xxSuccessful());

        mockMvc.perform(get("/api/user/me").header("Authorization", bearer))
                .andExpect(status().isUnauthorized());
    }
}
