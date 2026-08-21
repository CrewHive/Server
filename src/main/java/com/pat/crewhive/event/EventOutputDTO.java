package com.pat.crewhive.event;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Response DTO for an {@link Event}, exposed to the client instead of the JPA entity.
 */
public record EventOutputDTO(
        UUID eventId,
        String name,
        String description,
        OffsetDateTime start,
        OffsetDateTime end,
        LocalDate date,
        String color,
        String eventType,
        List<EventParticipantDTO> participants
) {

    public static EventOutputDTO from(Event event) {

        List<EventParticipantDTO> participants = event.getUsers().stream()
                .map(EventUsers::getUser)
                .map(u -> new EventParticipantDTO(u.getUserId(), u.getFirstName(), u.getLastName()))
                .collect(Collectors.toList());

        return new EventOutputDTO(
                event.getEventId(),
                event.getEventName(),
                event.getDescription(),
                event.getStart(),
                event.getEnd(),
                event.getDate(),
                event.getColor(),
                event.getEventType().getName(),
                participants
        );
    }
}
