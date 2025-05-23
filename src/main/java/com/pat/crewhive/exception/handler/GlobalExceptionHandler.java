package com.pat.crewhive.exception.handler;

import com.pat.crewhive.exception.custom.InvalidTokenException;
import com.pat.crewhive.exception.custom.ResourceAlreadyExistsException;
import com.pat.crewhive.exception.custom.ResourceNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    // @Valid
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidationException(MethodArgumentNotValidException ex) {
        Map<String, Object> errors = new HashMap<>();

        ex.getBindingResult().getFieldErrors().forEach(error -> {
            errors.put(error.getField(), error.getDefaultMessage());
        });

        log.error("Validation error: {}", errors);
        return ResponseEntity.status(400).body(Map.of("status", 400, "errors", errors));
    }


    // Problemi di parsing JSON con Jackson
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, Object>> handleParsingException(HttpMessageNotReadableException ex) {

        log.error("Parsing error: {}", ex.getMessage());
        return ResponseEntity.status(500).body(Map.of("status", 500, "error", ex.getMessage()));
    }


    // Token or Resource not found
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleResourceNotFoundException(ResourceNotFoundException ex) {

        log.error("Resource not found: {}", ex.getMessage());
        return ResponseEntity.status(404).body(Map.of("status", 404, "error", ex.getMessage()));
    }


    // BadCredentialsException
    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<Map<String, Object>> handleBadCredentialsException(BadCredentialsException ex) {

        log.error("Bad credentials: {}", ex.getMessage());
        return ResponseEntity.status(401).body(Map.of("status", 401, "error", ex.getMessage()));
    }


    // Resource already exists
    @ExceptionHandler(ResourceAlreadyExistsException.class)
    public ResponseEntity<Map<String, Object>> handleResourceAlreadyExistsException(ResourceAlreadyExistsException ex) {

        log.error("Resource already exists: {}", ex.getMessage());
        return ResponseEntity.status(500).body(Map.of("status", 401, "error", ex.getMessage()));
    }


    // InvalidTokenException
    @ExceptionHandler(InvalidTokenException.class)
    public ResponseEntity<Map<String, Object>> handleInvalidTokenException(InvalidTokenException ex) {

        log.error("Invalid token: {}", ex.getMessage());
        return ResponseEntity.status(400).body(Map.of("status", 400, "error", ex.getMessage()));
    }



    // IllegalStateException
    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalStateException(IllegalStateException ex) {

        log.error("Illegal state: {}", ex.getMessage());
        return ResponseEntity.status(500).body(Map.of("status", 500, "error", ex.getMessage()));
    }


    // RuntimeException
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<Map<String, Object>> handleRuntimeException(RuntimeException ex) {

        log.error("Runtime error: {}", ex.getMessage());
        return ResponseEntity.status(500).body(Map.of("status", 500, "error", ex.getMessage()));
    }


    // Generic Exception
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGenericException(Exception ex) {

        log.error("Generic error: {}", ex.getMessage());
        return ResponseEntity.status(500).body(Map.of("status", 500, "error", ex.getMessage()));
    }
}
