package com.pat.crewhive.security.exception.custom;

/**
 * Status: 404 Not Found
 */
public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String message) {
        super(message);
    }
}
