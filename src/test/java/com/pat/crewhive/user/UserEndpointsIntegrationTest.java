package com.pat.crewhive.user;

import com.jayway.jsonpath.JsonPath;
import com.pat.crewhive.common.PasswordUtil;
import com.pat.crewhive.company.Company;
import com.pat.crewhive.support.AbstractEndpointIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Endpoint tests for {@code /api/user/*}. Logout needs a real refresh token, so those tests log in
 * through the API and use the issued access token (JWT filter included).
 */
class UserEndpointsIntegrationTest extends AbstractEndpointIntegrationTest {

    private static final String PASSWORD = "Str0ng!Passw0rd";
    private static final String NEW_PASSWORD = "N3w!Str0ngPassw0rd";

    @Autowired
    private PasswordUtil passwordUtil;

    private Company alpha;
    private User employee;
    private User other;
    private User companyless;

    @BeforeEach
    void setUp() {
        alpha = newCompany("alpha");
        employee = userWithPassword("employee@alpha.test", alpha);
        other = userWithPassword("other@alpha.test", alpha);
        companyless = userWithPassword("free@example.test", null);
    }

    private User userWithPassword(String email, Company company) {
        User user = new User(email, "Mario", "Rossi", passwordUtil.encodePassword(PASSWORD));
        user.setCompany(company);
        return userRepository.saveAndFlush(user);
    }

    private org.springframework.test.web.servlet.ResultActions login(String email, String password) throws Exception {
        return mockMvc.perform(post("/api/auth/login").contentType(APPLICATION_JSON)
                .content(json(Map.of("email", email, "password", password))));
    }

    private String loginJson(String email) throws Exception {
        return login(email, PASSWORD).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
    }

    // ------------------------------------------------------------------ GET /me

    @Test
    void me_returnsTheCallersData() throws Exception {
        mockMvc.perform(get("/api/user/me").with(as(asUser(employee))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("employee@alpha.test"))
                .andExpect(jsonPath("$.companyName").value("alpha"))
                .andExpect(jsonPath("$.userId").value(employee.getUserId().toString()));
    }

    @Test
    void me_ofAUserWithoutCompany_hasNullCompanyName() throws Exception {
        mockMvc.perform(get("/api/user/me").with(as(asUser(companyless))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.companyName").doesNotExist());
    }

    @Test
    void me_withoutAuthentication_isUnauthorized() throws Exception {
        mockMvc.perform(get("/api/user/me")).andExpect(status().isUnauthorized());
    }

    @Test
    void me_withTheAccessTokenOfAUserWithoutCompany_works() throws Exception {
        String bearer = "Bearer " + JsonPath.read(loginJson("free@example.test"), "$.accessToken");

        mockMvc.perform(get("/api/user/me").header("Authorization", bearer))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("free@example.test"));
    }

    // ------------------------------------------------------------------ PATCH /update-password

    @Test
    void updatePassword_changesThePasswordUsedByLogin() throws Exception {
        mockMvc.perform(patch("/api/user/update-password").with(as(asUser(employee))).contentType(APPLICATION_JSON)
                        .content(json(Map.of("oldPassword", PASSWORD, "newPassword", NEW_PASSWORD))))
                .andExpect(status().isOk());

        login("employee@alpha.test", NEW_PASSWORD).andExpect(status().isOk());
        login("employee@alpha.test", PASSWORD).andExpect(status().isUnauthorized());
    }

    @Test
    void updatePassword_withWrongOldPassword_isRejectedAndKeepsThePassword() throws Exception {
        mockMvc.perform(patch("/api/user/update-password").with(as(asUser(employee))).contentType(APPLICATION_JSON)
                        .content(json(Map.of("oldPassword", "Wr0ng!Passw0rd!", "newPassword", NEW_PASSWORD))))
                .andExpect(status().isUnauthorized());

        login("employee@alpha.test", PASSWORD).andExpect(status().isOk());
    }

    @Test
    void updatePassword_withAWeakNewPassword_isRejectedAndKeepsThePassword() throws Exception {
        mockMvc.perform(patch("/api/user/update-password").with(as(asUser(employee))).contentType(APPLICATION_JSON)
                        .content(json(Map.of("oldPassword", PASSWORD, "newPassword", "weak"))))
                .andExpect(status().isBadRequest());

        login("employee@alpha.test", PASSWORD).andExpect(status().isOk());
    }

    @Test
    void updatePassword_withBlankFields_isBadRequest() throws Exception {
        mockMvc.perform(patch("/api/user/update-password").with(as(asUser(employee))).contentType(APPLICATION_JSON)
                        .content(json(Map.of("oldPassword", " ", "newPassword", NEW_PASSWORD))))
                .andExpect(status().isBadRequest());
        mockMvc.perform(patch("/api/user/update-password").with(as(asUser(employee))).contentType(APPLICATION_JSON)
                        .content(json(Map.of("oldPassword", PASSWORD))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void updatePassword_withoutAuthentication_isUnauthorized() throws Exception {
        mockMvc.perform(patch("/api/user/update-password").contentType(APPLICATION_JSON)
                        .content(json(Map.of("oldPassword", PASSWORD, "newPassword", NEW_PASSWORD))))
                .andExpect(status().isUnauthorized());
    }

    // ------------------------------------------------------------------ DELETE /leave-company

    @Test
    void leaveCompany_detachesTheUserAndReturnsNewTokens() throws Exception {
        mockMvc.perform(delete("/api/user/leave-company").with(as(asUser(employee))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.refreshToken").isNotEmpty());

        assertThat(jdbc.queryForObject("SELECT company_id FROM users WHERE user_id = ?", java.util.UUID.class, employee.getUserId())).isNull();
    }

    @Test
    void leaveCompany_withoutACompany_isNotFound() throws Exception {
        mockMvc.perform(delete("/api/user/leave-company").with(as(asUser(companyless))))
                .andExpect(status().isNotFound());
    }

    @Test
    void leaveCompany_withoutAuthentication_isUnauthorized() throws Exception {
        mockMvc.perform(delete("/api/user/leave-company")).andExpect(status().isUnauthorized());
    }

    // ------------------------------------------------------------------ DELETE /delete-account

    @Test
    void deleteAccount_deactivatesTheAccountAndBlocksTheLogin() throws Exception {
        mockMvc.perform(delete("/api/user/delete-account").with(as(asUser(other))))
                .andExpect(status().isOk());

        Boolean active = jdbc.queryForObject("SELECT active FROM users WHERE user_id = ?", Boolean.class, other.getUserId());
        assertThat(active).isFalse();
        login("other@alpha.test", PASSWORD).andExpect(status().isUnauthorized());
        // the colleague is untouched
        login("employee@alpha.test", PASSWORD).andExpect(status().isOk());
    }

    @Test
    void deleteAccount_withoutAuthentication_isUnauthorized() throws Exception {
        mockMvc.perform(delete("/api/user/delete-account")).andExpect(status().isUnauthorized());
    }

    // ------------------------------------------------------------------ POST /logout

    @Test
    void logout_revokesTheRefreshTokenFamily() throws Exception {
        String tokens = loginJson("employee@alpha.test");
        String bearer = "Bearer " + JsonPath.read(tokens, "$.accessToken");
        String refresh = JsonPath.read(tokens, "$.refreshToken");

        mockMvc.perform(post("/api/user/logout").header("Authorization", bearer).contentType(APPLICATION_JSON)
                        .content(json(Map.of("userId", employee.getUserId(), "refreshToken", refresh))))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/auth/rotate").contentType(APPLICATION_JSON)
                        .content(json(Map.of("refreshToken", refresh))))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void logout_withTheRefreshTokenOfAnotherUser_isUnauthorizedAndKeepsThatSession() throws Exception {
        String mine = loginJson("employee@alpha.test");
        String theirs = loginJson("other@alpha.test");
        String bearer = "Bearer " + JsonPath.read(mine, "$.accessToken");
        String theirRefresh = JsonPath.read(theirs, "$.refreshToken");

        mockMvc.perform(post("/api/user/logout").header("Authorization", bearer).contentType(APPLICATION_JSON)
                        .content(json(Map.of("userId", employee.getUserId(), "refreshToken", theirRefresh))))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(post("/api/auth/rotate").contentType(APPLICATION_JSON)
                        .content(json(Map.of("refreshToken", theirRefresh))))
                .andExpect(status().isOk());
    }

    @Test
    void logout_withBlankRefreshToken_isBadRequest() throws Exception {
        mockMvc.perform(post("/api/user/logout").with(as(asUser(employee))).contentType(APPLICATION_JSON)
                        .content(json(Map.of("userId", employee.getUserId(), "refreshToken", " "))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void logout_withoutAuthentication_isUnauthorized() throws Exception {
        mockMvc.perform(post("/api/user/logout").contentType(APPLICATION_JSON)
                        .content(json(Map.of("userId", employee.getUserId(), "refreshToken", "x"))))
                .andExpect(status().isUnauthorized());
    }
}
