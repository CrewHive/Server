package com.pat.crewhive.security.exception;

import com.pat.crewhive.security.exception.custom.InvalidRequestException;
import com.pat.crewhive.security.exception.custom.InvalidTokenException;
import com.pat.crewhive.security.exception.custom.JwtAuthenticationException;
import com.pat.crewhive.security.exception.custom.ResourceAlreadyExistsException;
import com.pat.crewhive.security.exception.custom.ResourceConflictException;
import com.pat.crewhive.security.exception.custom.ResourceNotFoundException;
import com.pat.crewhive.security.exception.handler.GlobalExceptionHandler;
import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.mock.http.MockHttpInputMessage;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link GlobalExceptionHandler}: status, errorCode and no leak of internal messages.
 */
class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void invalidRequestException_returnsItsMessageToTheClient() {
        ProblemDetail pd = handler.handleInvalidRequestException(
                new InvalidRequestException("L'inizio deve essere prima della fine"));

        assertThat(pd.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST.value());
        assertThat(pd.getDetail()).isEqualTo("L'inizio deve essere prima della fine");
        assertThat(pd.getProperties()).containsEntry("errorCode", "REQ_400");
    }

    @Test
    void illegalArgumentException_neverLeaksItsMessageToTheClient() {
        UUID userId = UUID.randomUUID();

        ProblemDetail pd = handler.handleIllegalArgumentException(
                new IllegalArgumentException("Invalid UUID string for user " + userId));

        assertThat(pd.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST.value());
        assertThat(pd.getDetail()).isEqualTo("Invalid request").doesNotContain(userId.toString());
        assertThat(pd.getProperties()).containsEntry("errorCode", "GEN_400_ILLARG");
    }

    // ---- gli altri handler ----

    private static void dummy(String ignored) {
    }

    @Test
    void validationException_returns400WithFieldErrorsOnly() throws Exception {
        BeanPropertyBindingResult binding = new BeanPropertyBindingResult(new Object(), "dto");
        binding.rejectValue(null, "x", "global");
        binding.addError(new org.springframework.validation.FieldError("dto", "name", "must not be blank"));
        MethodParameter parameter = new MethodParameter(
                GlobalExceptionHandlerTest.class.getDeclaredMethod("dummy", String.class), 0);

        ProblemDetail pd = handler.handleValidationException(new MethodArgumentNotValidException(parameter, binding));

        assertThat(pd.getStatus()).isEqualTo(400);
        assertThat(pd.getProperties()).containsEntry("errorCode", "VAL_400");
        assertThat(pd.getProperties().get("errors")).isEqualTo(Map.of("name", "must not be blank"));
    }

    @Test
    void authorizationDenied_returns403WithGenericDetail() {
        ProblemDetail pd = handler.handleAuthorizationDeniedException(new AuthorizationDeniedException("manager 42 not in company"));

        assertThat(pd.getStatus()).isEqualTo(403);
        assertThat(pd.getProperties()).containsEntry("errorCode", "AUTH_403_DENIED");
        assertThat(pd.getDetail()).doesNotContain("manager 42");
    }

    @Test
    void optimisticLockingFailure_returns409() {
        ProblemDetail pd = handler.handleOptimisticLockingFailure(new ObjectOptimisticLockingFailureException(Object.class, "id-1"));

        assertThat(pd.getStatus()).isEqualTo(409);
        assertThat(pd.getProperties()).containsEntry("errorCode", "DATA_409_OPTIMISTIC_LOCK");
    }

    @Test
    void unreadableBody_returns400WithoutParserDetails() {
        ProblemDetail pd = handler.handleParsingException(
                new HttpMessageNotReadableException("Unexpected token at line 3", new MockHttpInputMessage(new byte[0])));

        assertThat(pd.getStatus()).isEqualTo(400);
        assertThat(pd.getProperties()).containsEntry("errorCode", "JSON_400");
        assertThat(pd.getDetail()).isEqualTo("Body could not be parsed");
    }

    @Test
    void resourceNotFound_returns404WithoutTheInternalMessage() {
        UUID id = UUID.randomUUID();

        ProblemDetail pd = handler.handleResourceNotFoundException(new ResourceNotFoundException("Shift not found with ID: " + id));

        assertThat(pd.getStatus()).isEqualTo(404);
        assertThat(pd.getProperties()).containsEntry("errorCode", "RES_404");
        assertThat(pd.getDetail()).doesNotContain(id.toString());
    }

    @Test
    void resourceAlreadyExists_returns409WithoutTheInternalMessage() {
        ProblemDetail pd = handler.handleResourceAlreadyExistsException(new ResourceAlreadyExistsException("Company acme already exists"));

        assertThat(pd.getStatus()).isEqualTo(409);
        assertThat(pd.getProperties()).containsEntry("errorCode", "RES_409");
        assertThat(pd.getDetail()).doesNotContain("acme");
    }

    @Test
    void dataIntegrityViolation_returns409WithoutSqlDetails() {
        ProblemDetail pd = handler.onDataIntegrity(new DataIntegrityViolationException("duplicate key uk_users_email"));

        assertThat(pd.getStatus()).isEqualTo(409);
        assertThat(pd.getProperties()).containsEntry("errorCode", "DATA_409_INTEGRITY");
        assertThat(pd.getDetail()).doesNotContain("uk_users_email");
    }

    @Test
    void badCredentials_returns401WithUniformBody() {
        ProblemDetail pd = handler.handleBadCredentialsException(new BadCredentialsException("password mismatch for mario"));

        assertThat(pd.getStatus()).isEqualTo(401);
        assertThat(pd.getProperties()).containsEntry("errorCode", "AUTH_401_BAD_CREDENTIALS");
        assertThat(pd.getDetail()).isEqualTo("Unauthorized");
    }

    @Test
    void invalidToken_returns401WithoutTheInternalMessage() {
        ProblemDetail pd = handler.handleInvalidTokenException(new InvalidTokenException("Token expired on 2026-01-01"));

        assertThat(pd.getStatus()).isEqualTo(401);
        assertThat(pd.getProperties()).containsEntry("errorCode", "AUTH_401_INVALID_TOKEN");
        assertThat(pd.getDetail()).doesNotContain("2026");
    }

    @Test
    void jwtAuthenticationException_returns401() {
        ProblemDetail pd = handler.handleJwtAuthenticationException(new JwtAuthenticationException("bad signature"));

        assertThat(pd.getStatus()).isEqualTo(401);
        assertThat(pd.getProperties()).containsEntry("errorCode", "AUTH_401_JWT");
    }

    @Test
    void illegalState_returns500WithGenericDetail() {
        ProblemDetail pd = handler.handleIllegalStateException(new IllegalStateException("role 7 still assigned"));

        assertThat(pd.getStatus()).isEqualTo(500);
        assertThat(pd.getProperties()).containsEntry("errorCode", "GEN_500_ILLSTATE");
        assertThat(pd.getDetail()).isEqualTo("An unexpected error occurred");
    }

    @Test
    void runtimeException_returns500WithGenericDetail() {
        ProblemDetail pd = handler.handleRuntimeException(new RuntimeException("jdbc:postgresql://secret-host"));

        assertThat(pd.getStatus()).isEqualTo(500);
        assertThat(pd.getProperties()).containsEntry("errorCode", "GEN_500_RUNTIME");
        assertThat(pd.getDetail()).doesNotContain("secret-host");
    }

    @Test
    void checkedException_fallsBackTo500() {
        ProblemDetail pd = handler.handleGenericException(new Exception("boom"));

        assertThat(pd.getStatus()).isEqualTo(500);
        assertThat(pd.getProperties()).containsEntry("errorCode", "GEN_500");
        assertThat(pd.getDetail()).isEqualTo("An unexpected error occurred");
    }

    @Test
    void typeMismatch_returns400NamingTheParameterOnly() {
        MethodArgumentTypeMismatchException ex = new MethodArgumentTypeMismatchException(
                "not-a-uuid", UUID.class, "companyId", null, new IllegalArgumentException("Invalid UUID string: not-a-uuid"));

        ProblemDetail pd = handler.handleTypeMismatchException(ex);

        assertThat(pd.getStatus()).isEqualTo(400);
        assertThat(pd.getProperties()).containsEntry("errorCode", "REQ_400_TYPE");
        assertThat(pd.getDetail()).contains("companyId").doesNotContain("Invalid UUID string");
    }

    @Test
    void resourceConflict_returns409WithGenericDetail() {
        ProblemDetail pd = handler.handleResourceConflictException(new ResourceConflictException("role 7 is assigned to 3 users"));

        assertThat(pd.getStatus()).isEqualTo(409);
        assertThat(pd.getProperties()).containsEntry("errorCode", "RES_409_CONFLICT");
        assertThat(pd.getDetail()).doesNotContain("role 7");
    }
}
