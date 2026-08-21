package com.pat.crewhive.event;

import java.util.UUID;

/**
 * Lightweight representation of a user participating in an {@link Event},
 * used in {@link EventOutputDTO} instead of exposing the {@link com.pat.crewhive.user.User} entity.
 */
public record EventParticipantDTO(
        UUID userId,
        String firstName,
        String lastName
) {
}
