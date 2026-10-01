package com.pat.crewhive.security.exception.custom;

/**
 * Status: 409 Conflict.
 * The request is valid but the current state of the resource forbids it (e.g. deleting a role still
 * assigned to users). The message is only logged: the client gets a generic detail.
 */
public class ResourceConflictException extends RuntimeException {

    public ResourceConflictException(String message) {
        super(message);
    }
}
