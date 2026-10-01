package com.pat.crewhive.authuser;

import com.jayway.jsonpath.JsonPath;
import com.pat.crewhive.common.PasswordUtil;
import com.pat.crewhive.support.AbstractEndpointIntegrationTest;
import com.pat.crewhive.user.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.ResultActions;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Endpoint tests for the public {@code /api/auth/*} endpoints not covered by the registration and rotation
 * tests: login (uniform failure, M3) and the validation of the inputs.
 */
class AuthEndpointsIntegrationTest extends AbstractEndpointIntegrationTest {

    private static final String EMAIL = "mario.rossi@example.test";
    private static final String PASSWORD = "Str0ng!Passw0rd";

    @Autowired
    private PasswordUtil passwordUtil;

    private User user;

    @BeforeEach
    void setUp() {
        user = userRepository.saveAndFlush(new User(EMAIL, "Mario", "Rossi", passwordUtil.encodePassword(PASSWORD)));
    }

    private ResultActions login(Map<String, ?> body) throws Exception {
        return mockMvc.perform(post("/api/auth/login").contentType(APPLICATION_JSON).content(json(body)));
    }

    private String failureShape(ResultActions result) throws Exception {
        String body = result.andReturn().getResponse().getContentAsString();
        return result.andReturn().getResponse().getStatus() + "|" + JsonPath.read(body, "$.title") + "|"
                + JsonPath.read(body, "$.detail") + "|" + JsonPath.read(body, "$.errorCode");
    }

    // ------------------------------------------------------------------ login

    @Test
    void login_withValidCredentials_returnsBothTokens() throws Exception {
        login(Map.of("email", EMAIL, "password", PASSWORD))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.refreshToken").isNotEmpty());
    }

    @Test
    void login_isCaseInsensitiveOnTheEmail() throws Exception {
        login(Map.of("email", "Mario.Rossi@Example.TEST", "password", PASSWORD)).andExpect(status().isOk());
    }

    @Test
    void login_unknownUser_wrongPassword_andDeactivatedAccount_failWithTheSameResponse() throws Exception {
        User deactivated = userRepository.saveAndFlush(new User("gone@example.test", "Gino", "Bianchi", passwordUtil.encodePassword(PASSWORD)));
        jdbc.update("UPDATE users SET active = false WHERE user_id = ?", deactivated.getUserId());

        String wrongPassword = failureShape(login(Map.of("email", EMAIL, "password", "Wr0ng!Passw0rd!")).andExpect(status().isUnauthorized()));
        String unknown = failureShape(login(Map.of("email", "nobody@example.test", "password", PASSWORD)).andExpect(status().isUnauthorized()));
        String gone = failureShape(login(Map.of("email", "gone@example.test", "password", PASSWORD)).andExpect(status().isUnauthorized()));

        assertThat(unknown).isEqualTo(wrongPassword);
        assertThat(gone).isEqualTo(wrongPassword);
    }

    @Test
    void login_withInvalidBody_isBadRequest() throws Exception {
        login(Map.of("password", PASSWORD)).andExpect(status().isBadRequest());
        login(Map.of("email", EMAIL)).andExpect(status().isBadRequest());
        login(Map.of("email", " ", "password", PASSWORD)).andExpect(status().isBadRequest());
        mockMvc.perform(post("/api/auth/login").contentType(APPLICATION_JSON).content("{not json"))
                .andExpect(status().isBadRequest());
    }

    // ------------------------------------------------------------------ verify-email

    @Test
    void verifyEmail_withAnUnknownToken_isUnauthorizedAndCreatesNoUser() throws Exception {
        mockMvc.perform(post("/api/auth/verify-email").contentType(APPLICATION_JSON)
                        .content(json(Map.of("token", "unknown-token"))))
                .andExpect(status().isUnauthorized());

        Integer users = jdbc.queryForObject("SELECT count(*) FROM users", Integer.class);
        assertThat(users).isEqualTo(1);
    }

    @Test
    void verifyEmail_withBlankToken_isBadRequest() throws Exception {
        mockMvc.perform(post("/api/auth/verify-email").contentType(APPLICATION_JSON).content(json(Map.of("token", " "))))
                .andExpect(status().isBadRequest());
    }

    // ------------------------------------------------------------------ register

    @Test
    void register_withInvalidFields_isBadRequestAndSendsNoMail() throws Exception {
        Map<String, Object> badEmail = Map.of("email", "not-an-email", "firstName", "Mario", "lastName", "Rossi", "password", PASSWORD);
        Map<String, Object> shortPassword = Map.of("email", "new@example.test", "firstName", "Mario", "lastName", "Rossi", "password", "short");
        Map<String, Object> shortName = Map.of("email", "new@example.test", "firstName", "Ma", "lastName", "Rossi", "password", PASSWORD);
        Map<String, Object> htmlName = Map.of("email", "new@example.test", "firstName", "<b>Mario</b>", "lastName", "Rossi", "password", PASSWORD);

        for (Map<String, Object> body : java.util.List.of(badEmail, shortPassword, shortName, htmlName)) {
            mockMvc.perform(post("/api/auth/register").contentType(APPLICATION_JSON).content(json(body)))
                    .andExpect(status().isBadRequest());
        }
        org.mockito.Mockito.verifyNoInteractions(mailService);
    }

    // ------------------------------------------------------------------ rotate

    @Test
    void rotate_withUnknownOrMalformedToken_isUnauthorized() throws Exception {
        mockMvc.perform(post("/api/auth/rotate").contentType(APPLICATION_JSON)
                        .content(json(Map.of("refreshToken", java.util.UUID.randomUUID().toString()))))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(post("/api/auth/rotate").contentType(APPLICATION_JSON)
                        .content(json(Map.of("refreshToken", "not-a-uuid"))))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void rotate_withBlankToken_isBadRequest() throws Exception {
        mockMvc.perform(post("/api/auth/rotate").contentType(APPLICATION_JSON).content(json(Map.of("refreshToken", " "))))
                .andExpect(status().isBadRequest());
    }
}
