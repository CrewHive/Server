package com.pat.crewhive.company;

import com.pat.crewhive.support.AbstractEndpointIntegrationTest;
import com.pat.crewhive.user.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Endpoint tests for {@code /company/*} (register has its own test): happy path, validation,
 * authentication, role and tenant isolation.
 */
class CompanyEndpointsIntegrationTest extends AbstractEndpointIntegrationTest {

    private Company alpha;
    private Company beta;
    private User managerAlpha;
    private User employeeAlpha;
    private User managerBeta;
    private User employeeBeta;
    private User companyless;

    @BeforeEach
    void setUp() {
        alpha = newCompany("alpha");
        beta = newCompany("beta");
        managerAlpha = newUser("manager@alpha.test", alpha);
        employeeAlpha = newUser("employee@alpha.test", alpha);
        managerBeta = newUser("manager@beta.test", beta);
        employeeBeta = newUser("employee@beta.test", beta);
        companyless = newUser("free@example.test", null);
    }

    // ------------------------------------------------------------------ GET /{companyId}/users

    @Test
    void getUsers_asManagerOfTheCompany_listsOnlyItsUsers() throws Exception {
        mockMvc.perform(get("/company/{id}/users", alpha.getCompanyId()).with(as(asManager(managerAlpha))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[?(@.userId=='" + employeeAlpha.getUserId() + "')]", hasSize(1)))
                .andExpect(jsonPath("$[?(@.userId=='" + employeeBeta.getUserId() + "')]", hasSize(0)));
    }

    @Test
    void getUsers_asManagerOfAnotherCompany_isForbidden() throws Exception {
        mockMvc.perform(get("/company/{id}/users", alpha.getCompanyId()).with(as(asManager(managerBeta))))
                .andExpect(status().isForbidden());
    }

    @Test
    void getUsers_asPlainUser_isForbidden() throws Exception {
        mockMvc.perform(get("/company/{id}/users", alpha.getCompanyId()).with(as(asUser(employeeAlpha))))
                .andExpect(status().isForbidden());
    }

    @Test
    void getUsers_withoutAuthentication_isUnauthorized() throws Exception {
        mockMvc.perform(get("/company/{id}/users", alpha.getCompanyId())).andExpect(status().isUnauthorized());
    }

    @Test
    void getUsers_withMalformedCompanyId_isBadRequest() throws Exception {
        mockMvc.perform(get("/company/{id}/users", "not-a-uuid").with(as(asManager(managerAlpha))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("REQ_400_TYPE"));
    }

    // ------------------------------------------------------------------ GET /{companyId}/user/{targetId}/info

    @Test
    void getUserInfo_ofAnEmployeeOfTheCompany_returnsTheDetails() throws Exception {
        mockMvc.perform(get("/company/{c}/user/{t}/info", alpha.getCompanyId(), employeeAlpha.getUserId())
                        .with(as(asManager(managerAlpha))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("employee@alpha.test"))
                .andExpect(jsonPath("$.companyName").value("alpha"));
    }

    @Test
    void getUserInfo_ofAUserOfAnotherCompany_isNotFound() throws Exception {
        mockMvc.perform(get("/company/{c}/user/{t}/info", alpha.getCompanyId(), employeeBeta.getUserId())
                        .with(as(asManager(managerAlpha))))
                .andExpect(status().isNotFound());
    }

    @Test
    void getUserInfo_ofAnUnknownUser_isNotFoundLikeAForeignOne() throws Exception {
        mockMvc.perform(get("/company/{c}/user/{t}/info", alpha.getCompanyId(), UUID.randomUUID())
                        .with(as(asManager(managerAlpha))))
                .andExpect(status().isNotFound());
    }

    @Test
    void getUserInfo_asManagerOfAnotherCompany_isForbidden() throws Exception {
        mockMvc.perform(get("/company/{c}/user/{t}/info", alpha.getCompanyId(), employeeAlpha.getUserId())
                        .with(as(asManager(managerBeta))))
                .andExpect(status().isForbidden());
    }

    @Test
    void getUserInfo_asPlainUser_isForbidden() throws Exception {
        mockMvc.perform(get("/company/{c}/user/{t}/info", alpha.getCompanyId(), employeeAlpha.getUserId())
                        .with(as(asUser(employeeAlpha))))
                .andExpect(status().isForbidden());
    }

    // ------------------------------------------------------------------ PUT /set

    @Test
    void set_enrollsACompanylessUserInTheManagersCompany() throws Exception {
        mockMvc.perform(put("/company/set").with(as(asManager(managerAlpha))).contentType(APPLICATION_JSON)
                        .content(json(Map.of("companyName", "alpha", "userId", companyless.getUserId()))))
                .andExpect(status().isOk());

        UUID companyId = jdbc.queryForObject("SELECT company_id FROM users WHERE user_id = ?", UUID.class, companyless.getUserId());
        assertThat(companyId).isEqualTo(alpha.getCompanyId());
    }

    @Test
    void set_withTheNameOfAnotherCompany_isForbiddenAndEnrollsNobody() throws Exception {
        mockMvc.perform(put("/company/set").with(as(asManager(managerAlpha))).contentType(APPLICATION_JSON)
                        .content(json(Map.of("companyName", "beta", "userId", companyless.getUserId()))))
                .andExpect(status().isForbidden());

        UUID companyId = jdbc.queryForObject("SELECT company_id FROM users WHERE user_id = ?", UUID.class, companyless.getUserId());
        assertThat(companyId).isNull();
    }

    @Test
    void set_withBlankName_isBadRequest() throws Exception {
        mockMvc.perform(put("/company/set").with(as(asManager(managerAlpha))).contentType(APPLICATION_JSON)
                        .content(json(Map.of("companyName", " ", "userId", companyless.getUserId()))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void set_withoutUserId_isBadRequest() throws Exception {
        mockMvc.perform(put("/company/set").with(as(asManager(managerAlpha))).contentType(APPLICATION_JSON)
                        .content(json(Map.of("companyName", "alpha"))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void set_asPlainUser_isForbidden() throws Exception {
        mockMvc.perform(put("/company/set").with(as(asUser(employeeAlpha))).contentType(APPLICATION_JSON)
                        .content(json(Map.of("companyName", "alpha", "userId", companyless.getUserId()))))
                .andExpect(status().isForbidden());
    }

    @Test
    void set_withoutAuthentication_isUnauthorized() throws Exception {
        mockMvc.perform(put("/company/set").contentType(APPLICATION_JSON)
                        .content(json(Map.of("companyName", "alpha", "userId", companyless.getUserId()))))
                .andExpect(status().isUnauthorized());
    }

    // ------------------------------------------------------------------ DELETE /{companyId}/remove/{userId}

    @Test
    void remove_detachesTheEmployeeFromTheCompany() throws Exception {
        mockMvc.perform(delete("/company/{c}/remove/{u}", alpha.getCompanyId(), employeeAlpha.getUserId())
                        .with(as(asManager(managerAlpha))))
                .andExpect(status().isOk());

        UUID companyId = jdbc.queryForObject("SELECT company_id FROM users WHERE user_id = ?", UUID.class, employeeAlpha.getUserId());
        assertThat(companyId).isNull();
    }

    @Test
    void remove_aManagerCannotRemoveThemselves() throws Exception {
        mockMvc.perform(delete("/company/{c}/remove/{u}", alpha.getCompanyId(), managerAlpha.getUserId())
                        .with(as(asManager(managerAlpha))))
                .andExpect(status().isForbidden());
    }

    @Test
    void remove_aUserOfAnotherCompany_isNotFoundAndStaysWhereItIs() throws Exception {
        mockMvc.perform(delete("/company/{c}/remove/{u}", alpha.getCompanyId(), employeeBeta.getUserId())
                        .with(as(asManager(managerAlpha))))
                .andExpect(status().isNotFound());

        UUID companyId = jdbc.queryForObject("SELECT company_id FROM users WHERE user_id = ?", UUID.class, employeeBeta.getUserId());
        assertThat(companyId).isEqualTo(beta.getCompanyId());
    }

    @Test
    void remove_asManagerOfAnotherCompany_isForbidden() throws Exception {
        mockMvc.perform(delete("/company/{c}/remove/{u}", alpha.getCompanyId(), employeeAlpha.getUserId())
                        .with(as(asManager(managerBeta))))
                .andExpect(status().isForbidden());
    }

    @Test
    void remove_asPlainUser_isForbidden() throws Exception {
        mockMvc.perform(delete("/company/{c}/remove/{u}", alpha.getCompanyId(), managerAlpha.getUserId())
                        .with(as(asUser(employeeAlpha))))
                .andExpect(status().isForbidden());
    }

    @Test
    void remove_withoutAuthentication_isUnauthorized() throws Exception {
        mockMvc.perform(delete("/company/{c}/remove/{u}", alpha.getCompanyId(), employeeAlpha.getUserId()))
                .andExpect(status().isUnauthorized());
    }

    // ------------------------------------------------------------------ DELETE /{companyId}/delete

    @Test
    void delete_softDeletesTheCompanyAndDetachesItsUsers() throws Exception {
        mockMvc.perform(delete("/company/{c}/delete", alpha.getCompanyId()).with(as(asManager(managerAlpha))))
                .andExpect(status().isOk());

        Boolean active = jdbc.queryForObject("SELECT active FROM company WHERE company_id = ?", Boolean.class, alpha.getCompanyId());
        assertThat(active).isFalse();
        Integer stillAttached = jdbc.queryForObject(
                "SELECT count(*) FROM users WHERE company_id = ?", Integer.class, alpha.getCompanyId());
        assertThat(stillAttached).isZero();
        // the other tenant is untouched
        Boolean betaActive = jdbc.queryForObject("SELECT active FROM company WHERE company_id = ?", Boolean.class, beta.getCompanyId());
        assertThat(betaActive).isTrue();
    }

    @Test
    void delete_asManagerOfAnotherCompany_isForbiddenAndDeletesNothing() throws Exception {
        mockMvc.perform(delete("/company/{c}/delete", alpha.getCompanyId()).with(as(asManager(managerBeta))))
                .andExpect(status().isForbidden());

        Boolean active = jdbc.queryForObject("SELECT active FROM company WHERE company_id = ?", Boolean.class, alpha.getCompanyId());
        assertThat(active).isTrue();
    }

    @Test
    void delete_asPlainUser_isForbidden() throws Exception {
        mockMvc.perform(delete("/company/{c}/delete", alpha.getCompanyId()).with(as(asUser(employeeAlpha))))
                .andExpect(status().isForbidden());
    }

    @Test
    void delete_withoutAuthentication_isUnauthorized() throws Exception {
        mockMvc.perform(delete("/company/{c}/delete", alpha.getCompanyId())).andExpect(status().isUnauthorized());
    }
}
