package com.pat.crewhive.shiftprogrammed;

import java.util.UUID;

/**
 * Lightweight representation of a user assigned to a {@link ShiftProgrammed},
 * used instead of exposing the {@link com.pat.crewhive.user.User} entity.
 */
public record ShiftParticipantDTO(
        UUID userId,
        String firstName,
        String lastName
) {
}
