package com.pat.hours_calculator.exception;


import jakarta.persistence.EntityNotFoundException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class TokenExceptionHandler {

    // Token not found
    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleEntityNotFoundException(EntityNotFoundException ex) {

        return ResponseEntity.status(404).body(Map.of("status", 404, "error", ex.getMessage()));
    }

    // IllegalStateException
    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalStateException(IllegalStateException ex) {

        return ResponseEntity.status(500).body(Map.of("status", 500, "error", ex.getMessage()));
    }
}
