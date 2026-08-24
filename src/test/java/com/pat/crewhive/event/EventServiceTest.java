package com.pat.crewhive.event;

import com.pat.crewhive.common.DateUtils;
import com.pat.crewhive.common.Period;
import com.pat.crewhive.common.StringUtils;
import com.pat.crewhive.user.User;
import com.pat.crewhive.user.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link EventService}.
 */
@ExtendWith(MockitoExtension.class)
class EventServiceTest {

    @Mock
    private EventRepository eventRepository;
    @Mock
    private EventUsersRepository eventUsersRepository;
    @Mock
    private EventTypeRepository eventTypeRepository;
    @Mock
    private UserService userService;
    @Mock
    private StringUtils stringUtils;
    @Mock
    private DateUtils dateUtils;

    private EventService eventService;

    @BeforeEach
    void setUp() {
        eventService = new EventService(
                eventRepository, eventUsersRepository, eventTypeRepository, userService, stringUtils, dateUtils
        );
    }

    private Event buildEventWithParticipant() {
        Event event = new Event();
        ReflectionTestUtils.setField(event, "eventId", UUID.randomUUID());
        event.setEventName("Riunione");
        event.setDescription("Riunione di team");
        event.setStart(OffsetDateTime.parse("2026-08-21T09:00:00Z"));
        event.setEnd(OffsetDateTime.parse("2026-08-21T17:00:00Z"));
        event.setColor("#FF0000");
        event.setEventType(new EventTypeEntity((short) 2, "Private"));

        User user = new User("user@example.com", "Mario", "Rossi", "encoded-pwd");
        ReflectionTestUtils.setField(user, "userId", UUID.randomUUID());
        event.addUser(user);

        return event;
    }

    @Test
    void getEventsByPeriodAndUser_returnsEventsMappedToOutputDTOs() {
        UUID userId = UUID.randomUUID();
        LocalDate from = LocalDate.of(2026, 8, 17);
        LocalDate to = LocalDate.of(2026, 8, 23);
        Event event = buildEventWithParticipant();

        when(dateUtils.getStartDateForPeriod(Period.WEEK)).thenReturn(from);
        when(dateUtils.getEndDateForPeriod(Period.WEEK)).thenReturn(to);
        when(eventRepository.findWithParticipantsByUserAndDateBetween(userId, from, to))
                .thenReturn(List.of(event));

        List<EventOutputDTO> result = eventService.getEventsByPeriodAndUser(Period.WEEK, userId);

        assertThat(result).containsExactly(EventOutputDTO.from(event));
    }

    @Test
    void getUserEvents_returnsEventsMappedToOutputDTOs() {
        UUID userId = UUID.randomUUID();
        Event event = buildEventWithParticipant();

        when(eventUsersRepository.findEventsByUserId(userId)).thenReturn(List.of(event));

        List<EventOutputDTO> result = eventService.getUserEvents(userId);

        assertThat(result).containsExactly(EventOutputDTO.from(event));
    }

    @Test
    void getPublicEventsByCompanyAndPeriod_returnsEventsMappedToOutputDTOs() {
        UUID companyId = UUID.randomUUID();
        LocalDate from = LocalDate.of(2026, 8, 17);
        LocalDate to = LocalDate.of(2026, 8, 23);
        Event event = buildEventWithParticipant();

        when(dateUtils.getStartDateForPeriod(Period.WEEK)).thenReturn(from);
        when(dateUtils.getEndDateForPeriod(Period.WEEK)).thenReturn(to);
        when(eventRepository.findPublicWithParticipantsByCompanyAndDateBetween(EventType.PUBLIC.getId(), companyId, from, to))
                .thenReturn(List.of(event));

        List<EventOutputDTO> result = eventService.getPublicEventsByCompanyAndPeriod(companyId, Period.WEEK);

        assertThat(result).containsExactly(EventOutputDTO.from(event));
    }

    @Test
    void patchEvent_reactivatesPreviouslySoftDeletedLink_insteadOfCreatingDuplicate() {
        UUID eventId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        Event event = new Event();
        ReflectionTestUtils.setField(event, "eventId", eventId);
        event.setEventName("Riunione");
        event.setStart(OffsetDateTime.parse("2026-08-21T09:00:00Z"));
        event.setEnd(OffsetDateTime.parse("2026-08-21T17:00:00Z"));
        event.setColor("#FF0000");

        User user = new User("user@example.com", "Mario", "Rossi", "encoded-pwd");
        ReflectionTestUtils.setField(user, "userId", userId);

        EventUsers softDeletedLink = new EventUsers(user, event);
        softDeletedLink.markDeleted(user);

        PatchEventDTO dto = new PatchEventDTO(
                eventId, "Riunione", null,
                OffsetDateTime.parse("2026-08-21T09:00:00Z"),
                OffsetDateTime.parse("2026-08-21T17:00:00Z"),
                "FF0000", EventType.PRIVATE, java.util.Set.of(userId)
        );

        when(eventRepository.findByIdWithParticipants(eventId)).thenReturn(java.util.Optional.of(event));
        when(eventTypeRepository.getReferenceById((short) 2)).thenReturn(new EventTypeEntity((short) 2, "Private"));
        when(userService.getUsersByIds(java.util.Set.of(userId))).thenReturn(List.of(user));
        when(eventUsersRepository.findByIdIncludingDeleted(userId, eventId)).thenReturn(java.util.Optional.of(softDeletedLink));
        when(stringUtils.normalizeString("Riunione")).thenReturn("Riunione");
        when(eventRepository.save(event)).thenReturn(event);

        eventService.patchEvent(dto);

        assertThat(softDeletedLink.isActive()).isTrue();
        assertThat(softDeletedLink.getDeletedAt()).isNull();
        assertThat(softDeletedLink.getDeletedBy()).isNull();
        assertThat(event.getUsers()).containsExactly(softDeletedLink);
    }

    @Test
    void createEvent_persistsAllParticipants_notJustTheFirst() {
        User user1 = new User("user1@example.com", "Mario", "Rossi", "encoded-pwd");
        ReflectionTestUtils.setField(user1, "userId", UUID.randomUUID());
        User user2 = new User("user2@example.com", "Luigi", "Verdi", "encoded-pwd");
        ReflectionTestUtils.setField(user2, "userId", UUID.randomUUID());

        CreateEventDTO dto = new CreateEventDTO(
                "Riunione", null,
                OffsetDateTime.parse("2026-08-21T09:00:00Z"),
                OffsetDateTime.parse("2026-08-21T17:00:00Z"),
                "FF0000", EventType.PRIVATE, Set.of(user1.getUserId(), user2.getUserId())
        );

        when(stringUtils.normalizeString("Riunione")).thenReturn("Riunione");
        when(userService.getUsersByIds(Set.of(user1.getUserId(), user2.getUserId())))
                .thenReturn(List.of(user1, user2));
        when(eventTypeRepository.getReferenceById((short) 2)).thenReturn(new EventTypeEntity((short) 2, "Private"));
        when(eventRepository.save(any(Event.class))).thenAnswer(inv -> inv.getArgument(0));

        eventService.createEvent(dto, Collections.singleton("ROLE_USER"));

        ArgumentCaptor<Event> captor = ArgumentCaptor.forClass(Event.class);
        verify(eventRepository).save(captor.capture());
        assertThat(captor.getValue().getUsers()).hasSize(2);
    }
}
