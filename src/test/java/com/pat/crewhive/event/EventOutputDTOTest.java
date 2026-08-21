package com.pat.crewhive.event;

import com.pat.crewhive.user.User;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link EventOutputDTO#from(Event)}.
 */
class EventOutputDTOTest {

    private User buildUser(UUID userId, String firstName, String lastName) {
        User user = new User("user@example.com", firstName, lastName, "encoded-pwd");
        ReflectionTestUtils.setField(user, "userId", userId);
        return user;
    }

    @Test
    void from_mapsEventFieldsAndParticipants() {
        UUID eventId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        OffsetDateTime start = OffsetDateTime.parse("2026-08-21T09:00:00Z");
        OffsetDateTime end = OffsetDateTime.parse("2026-08-21T17:00:00Z");

        Event event = new Event();
        ReflectionTestUtils.setField(event, "eventId", eventId);
        event.setEventName("Riunione");
        event.setDescription("Riunione di team");
        event.setStart(start);
        event.setEnd(end);
        ReflectionTestUtils.setField(event, "date", LocalDate.of(2026, 8, 21));
        event.setColor("#FF0000");
        event.setEventType(new EventTypeEntity((short) 2, "Private"));

        User user = buildUser(userId, "Mario", "Rossi");
        event.addUser(user);

        EventOutputDTO dto = EventOutputDTO.from(event);

        assertThat(dto.eventId()).isEqualTo(eventId);
        assertThat(dto.name()).isEqualTo("Riunione");
        assertThat(dto.description()).isEqualTo("Riunione di team");
        assertThat(dto.start()).isEqualTo(start);
        assertThat(dto.end()).isEqualTo(end);
        assertThat(dto.date()).isEqualTo(LocalDate.of(2026, 8, 21));
        assertThat(dto.color()).isEqualTo("#FF0000");
        assertThat(dto.eventType()).isEqualTo("Private");
        assertThat(dto.participants())
                .containsExactly(new EventParticipantDTO(userId, "Mario", "Rossi"));
    }

    @Test
    void from_eventWithoutParticipants_returnsEmptyParticipantsList() {
        Event event = new Event();
        ReflectionTestUtils.setField(event, "eventId", UUID.randomUUID());
        event.setEventName("Solo");
        event.setStart(OffsetDateTime.now());
        event.setEnd(OffsetDateTime.now().plusHours(1));
        event.setColor("#00FF00");
        event.setEventType(new EventTypeEntity((short) 1, "Public"));

        EventOutputDTO dto = EventOutputDTO.from(event);

        assertThat(dto.participants()).isEmpty();
    }
}
