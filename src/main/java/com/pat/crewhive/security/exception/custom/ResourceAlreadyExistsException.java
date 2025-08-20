package com.pat.crewhive.security.exception.custom;

/**
 * Status: 409 Conflict
 */
public class ResourceAlreadyExistsException extends RuntimeException {
    public ResourceAlreadyExistsException(String message) {
        super(message);
    }
}
