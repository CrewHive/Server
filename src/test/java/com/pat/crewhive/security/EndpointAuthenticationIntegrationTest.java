package com.pat.crewhive.security;

import com.pat.crewhive.support.AbstractEndpointIntegrationTest;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Cross-cutting check: every non-public endpoint rejects a request without a valid access token with 401,
 * and the public ones are reachable without it. Unauthenticated requests never reach the controller, so the
 * bodies are irrelevant.
 */
class EndpointAuthenticationIntegrationTest extends AbstractEndpointIntegrationTest {

    private static final String ID = "00000000-0000-0000-0000-000000000001";

    @ParameterizedTest(name = "{0} {1} without token -> 401")
    @CsvSource({
            "GET,    /api/user/me",
            "POST,   /api/user/logout",
            "PATCH,  /api/user/update-password",
            "DELETE, /api/user/leave-company",
            "DELETE, /api/user/delete-account",
            "GET,    /company/" + ID + "/users",
            "GET,    /company/" + ID + "/user/" + ID + "/info",
            "POST,   /company/register",
            "PUT,    /company/set",
            "DELETE, /company/" + ID + "/remove/" + ID,
            "DELETE, /company/" + ID + "/delete",
            "POST,   /event/create",
            "GET,    /event/DAY/user/" + ID,
            "GET,    /event/user/" + ID,
            "GET,    /event/public/DAY",
            "PATCH,  /event/patch",
            "DELETE, /event/delete/" + ID,
            "GET,    /event/invitations",
            "POST,   /event/" + ID + "/respond",
            "POST,   /manager/create-role",
            "PATCH,  /manager/update-user-role",
            "PATCH,  /manager/update-user-work-info",
            "DELETE, /manager/delete-role/cashier",
            "POST,   /shift-programmed/create",
            "GET,    /shift-programmed/period/DAY/user/" + ID,
            "GET,    /shift-programmed/period/DAY/" + ID,
            "GET,    /shift-programmed/users/" + ID,
            "PATCH,  /shift-programmed/patch",
            "DELETE, /shift-programmed/delete/" + ID,
            "GET,    /shift-template/get/morning",
            "POST,   /shift-template/create",
            "PATCH,  /shift-template/update",
            "DELETE, /shift-template/delete/morning",
            "POST,   /shift-worked/create",
            "GET,    /docs"
    })
    void protectedEndpoints_withoutToken_areUnauthorized(String method, String path) throws Exception {
        mockMvc.perform(request(HttpMethod.valueOf(method), path)).andExpect(status().isUnauthorized());
    }

    @ParameterizedTest(name = "{0} {1} with an invalid token -> 401")
    @CsvSource({
            "GET,    /api/user/me",
            "GET,    /company/" + ID + "/users",
            "POST,   /event/create",
            "POST,   /manager/create-role",
            "GET,    /shift-programmed/users/" + ID,
            "POST,   /shift-worked/create"
    })
    void protectedEndpoints_withAnInvalidToken_areUnauthorized(String method, String path) throws Exception {
        mockMvc.perform(request(HttpMethod.valueOf(method), path).header("Authorization", "Bearer not.a.jwt"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void docs_asAPlainUser_isForbidden() throws Exception {
        mockMvc.perform(request(HttpMethod.GET, "/docs").with(as(principal(UUID.randomUUID(), null, "ROLE_USER"))))
                .andExpect(status().isForbidden());
    }

    @Test
    void publicAuthEndpoints_areReachableWithoutToken() throws Exception {
        // an empty body is rejected by validation (400): what matters is that it is not 401/403
        for (String path : new String[]{"/api/auth/login", "/api/auth/register", "/api/auth/verify-email", "/api/auth/rotate"}) {
            int status = mockMvc.perform(request(HttpMethod.POST, path).contentType("application/json").content("{}"))
                    .andReturn().getResponse().getStatus();
            assertThat(status).as(path).isEqualTo(400);
        }
    }
}
