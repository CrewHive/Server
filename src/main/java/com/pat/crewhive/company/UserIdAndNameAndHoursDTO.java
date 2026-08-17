package com.pat.crewhive.company;

import java.util.UUID;

public record UserIdAndNameAndHoursDTO(
        UUID userId,
        String firstName,
        String lastName,
        int workableHoursPerWeek
) {
}
