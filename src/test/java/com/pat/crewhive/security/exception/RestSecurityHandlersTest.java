package com.pat.crewhive.security.exception;

import com.pat.crewhive.security.exception.handler.RestAccessDeniedHandler;
import com.pat.crewhive.security.exception.handler.RestAuthenticationEntryPoint;
import org.junit.jupiter.api.Test;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.json.ProblemDetailJacksonMixin;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for the JSON bodies written by {@link RestAccessDeniedHandler} and {@link RestAuthenticationEntryPoint}.
 */
class RestSecurityHandlersTest {

    // come il mapper dell'app: Spring registra il mixin che appiattisce le properties del ProblemDetail
    private final ObjectMapper mapper = JsonMapper.builder()
            .addMixIn(ProblemDetail.class, ProblemDetailJacksonMixin.class)
            .build();

    @Test
    void accessDeniedHandler_writes403ProblemJsonWithoutLeakingTheException() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/manager/create-role");
        MockHttpServletResponse response = new MockHttpServletResponse();

        new RestAccessDeniedHandler(mapper).handle(request, response, new AccessDeniedException("secret internal detail"));

        JsonNode body = mapper.readTree(response.getContentAsString());
        assertThat(response.getStatus()).isEqualTo(403);
        assertThat(response.getContentType()).startsWith("application/json");
        assertThat(body.get("status").asInt()).isEqualTo(403);
        assertThat(body.get("errorCode").asString()).isEqualTo("AUTH_403");
        assertThat(body.get("path").asString()).isEqualTo("/manager/create-role");
        assertThat(body.get("timestamp").asString()).isNotBlank();
        assertThat(response.getContentAsString()).doesNotContain("secret internal detail");
    }

    @Test
    void authenticationEntryPoint_writes401ProblemJsonWithoutLeakingTheException() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/user/me");
        MockHttpServletResponse response = new MockHttpServletResponse();

        new RestAuthenticationEntryPoint(mapper).commence(request, response, new BadCredentialsException("secret internal detail"));

        JsonNode body = mapper.readTree(response.getContentAsString());
        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(response.getContentType()).startsWith("application/json");
        assertThat(body.get("status").asInt()).isEqualTo(401);
        assertThat(body.get("errorCode").asString()).isEqualTo("AUTH_401");
        assertThat(body.get("path").asString()).isEqualTo("/api/user/me");
        assertThat(response.getContentAsString()).doesNotContain("secret internal detail");
    }
}
