package com.pat.crewhive.security.exception.custom;

/**
 * Status: 400 Bad Request.
 * The message is returned as-is to the client: it must never contain identifiers or internal details.
 */
public class InvalidRequestException extends RuntimeException {

    public InvalidRequestException(String message) {
        super(message);
    }
}
