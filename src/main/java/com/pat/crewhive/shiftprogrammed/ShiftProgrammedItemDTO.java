package com.pat.crewhive.shiftprogrammed;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Response DTO for a {@link ShiftProgrammed}, exposed to the client instead of the JPA entity.
 */
public record ShiftProgrammedItemDTO(
        UUID shiftProgrammedId,
        String shiftName,
        String description,
        OffsetDateTime start,
        OffsetDateTime end,
        LocalDate date,
        String color,
        List<ShiftParticipantDTO> users
) {

    public static ShiftProgrammedItemDTO from(ShiftProgrammed shift) {

        List<ShiftParticipantDTO> users = shift.getUsers().stream()
                .map(ShiftUser::getUser)
                .map(u -> new ShiftParticipantDTO(u.getUserId(), u.getFirstName(), u.getLastName()))
                .collect(Collectors.toList());

        return new ShiftProgrammedItemDTO(
                shift.getShiftProgrammedId(),
                shift.getShiftName(),
                shift.getDescription(),
                shift.getStart(),
                shift.getEnd(),
                shift.getDate(),
                shift.getColor(),
                users
        );
    }
}
