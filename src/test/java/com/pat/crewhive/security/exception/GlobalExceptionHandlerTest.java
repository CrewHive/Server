package com.pat.crewhive.security.exception;

import com.pat.crewhive.security.exception.custom.InvalidRequestException;
import com.pat.crewhive.security.exception.handler.GlobalExceptionHandler;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for the 400 handlers of {@link GlobalExceptionHandler} (L6).
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
}
