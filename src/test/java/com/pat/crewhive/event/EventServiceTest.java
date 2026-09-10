package com.pat.crewhive.event;

import com.pat.crewhive.common.DateUtils;
import com.pat.crewhive.common.Period;
import com.pat.crewhive.common.StringUtils;
import com.pat.crewhive.company.Company;
import com.pat.crewhive.user.User;
import com.pat.crewhive.user.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link EventService}.
 */
@ExtendWith(MockitoExtension.class)
class EventServiceTest {

    private static final UUID COMPANY_A = UUID.randomUUID();
    private static final UUID COMPANY_B = UUID.randomUUID();

    private static final OffsetDateTime START = OffsetDateTime.parse("2026-08-21T09:00:00Z");
    private static final OffsetDateTime END = OffsetDateTime.parse("2026-08-21T17:00:00Z");

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

    // ---------------------------------------------------------------------
    // helpers
    // ---------------------------------------------------------------------

    private Company company(UUID companyId) {
        Company c = new Company();
        ReflectionTestUtils.setField(c, "companyId", companyId);
        return c;
    }

    private User user(UUID userId, UUID companyId) {
        User u = new User("user-" + userId + "@example.com", "Mario", "Rossi", "encoded-pwd");
        ReflectionTestUtils.setField(u, "userId", userId);
        u.setCompany(company(companyId));
        return u;
    }

    private Event event(UUID eventId, User creator) {
        Event event = new Event();
        ReflectionTestUtils.setField(event, "eventId", eventId);
        event.setEventName("Riunione");
        event.setDescription("Riunione di team");
        event.setStart(START);
        event.setEnd(END);
        event.setColor("#FF0000");
        event.setEventType(new EventTypeEntity((short) 2, "Private"));
        event.setCreator(creator);
        return event;
    }

    private Event buildEventWithParticipant() {
        User creator = user(UUID.randomUUID(), COMPANY_A);
        Event event = event(UUID.randomUUID(), creator);
        event.addUser(creator);
        return event;
    }

    private PatchEventDTO patchDto(UUID eventId, Set<UUID> participants) {
        return new PatchEventDTO(eventId, "Riunione", null, START, END, "FF0000", EventType.PRIVATE, participants);
    }

    // ---------------------------------------------------------------------
    // read paths (unchanged behaviour)
    // ---------------------------------------------------------------------

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

    // ---------------------------------------------------------------------
    // createEvent
    // ---------------------------------------------------------------------

    @Test
    void createEvent_setsCreatorFromPrincipal_andAddsCreatorAsParticipant() {
        User creator = user(UUID.randomUUID(), COMPANY_A);
        User p1 = user(UUID.randomUUID(), COMPANY_A);
        User p2 = user(UUID.randomUUID(), COMPANY_A);

        CreateEventDTO dto = new CreateEventDTO(
                "Riunione", null, START, END, "FF0000", EventType.PRIVATE,
                Set.of(p1.getUserId(), p2.getUserId()));

        when(stringUtils.normalizeString("Riunione")).thenReturn("Riunione");
        when(userService.getUserById(creator.getUserId())).thenReturn(creator);
        when(userService.getUsersByIds(Set.of(p1.getUserId(), p2.getUserId()))).thenReturn(List.of(p1, p2));
        when(eventTypeRepository.getReferenceById((short) 2)).thenReturn(new EventTypeEntity((short) 2, "Private"));
        when(eventRepository.save(any(Event.class))).thenAnswer(inv -> inv.getArgument(0));

        eventService.createEvent(dto, creator.getUserId(), COMPANY_A, Collections.singleton("ROLE_USER"));

        ArgumentCaptor<Event> captor = ArgumentCaptor.forClass(Event.class);
        verify(eventRepository).save(captor.capture());
        Event saved = captor.getValue();

        assertThat(saved.getCreator()).isSameAs(creator);
        assertThat(saved.getUsers())
                .extracting(eu -> eu.getUser().getUserId())
                .containsExactlyInAnyOrder(p1.getUserId(), p2.getUserId(), creator.getUserId());
    }

    @Test
    void createEvent_deniesWhenAParticipantBelongsToAnotherCompany() {
        User creator = user(UUID.randomUUID(), COMPANY_A);
        User foreign = user(UUID.randomUUID(), COMPANY_B);

        CreateEventDTO dto = new CreateEventDTO(
                "Riunione", null, START, END, "FF0000", EventType.PRIVATE, Set.of(foreign.getUserId()));

        when(stringUtils.normalizeString("Riunione")).thenReturn("Riunione");
        when(userService.getUserById(creator.getUserId())).thenReturn(creator);
        when(userService.getUsersByIds(Set.of(foreign.getUserId()))).thenReturn(List.of(foreign));

        assertThatThrownBy(() ->
                eventService.createEvent(dto, creator.getUserId(), COMPANY_A, Collections.singleton("ROLE_USER")))
                .isInstanceOf(AuthorizationDeniedException.class);

        verify(eventRepository, never()).save(any());
    }

    @Test
    void createEvent_persistsAllParticipants_notJustTheFirst() {
        User creator = user(UUID.randomUUID(), COMPANY_A);
        User user1 = user(UUID.randomUUID(), COMPANY_A);
        User user2 = user(UUID.randomUUID(), COMPANY_A);

        CreateEventDTO dto = new CreateEventDTO(
                "Riunione", null, START, END, "FF0000", EventType.PRIVATE,
                Set.of(user1.getUserId(), user2.getUserId()));

        when(stringUtils.normalizeString("Riunione")).thenReturn("Riunione");
        when(userService.getUserById(creator.getUserId())).thenReturn(creator);
        when(userService.getUsersByIds(Set.of(user1.getUserId(), user2.getUserId())))
                .thenReturn(List.of(user1, user2));
        when(eventTypeRepository.getReferenceById((short) 2)).thenReturn(new EventTypeEntity((short) 2, "Private"));
        when(eventRepository.save(any(Event.class))).thenAnswer(inv -> inv.getArgument(0));

        eventService.createEvent(dto, creator.getUserId(), COMPANY_A, Collections.singleton("ROLE_USER"));

        ArgumentCaptor<Event> captor = ArgumentCaptor.forClass(Event.class);
        verify(eventRepository).save(captor.capture());
        assertThat(captor.getValue().getUsers()).hasSize(3);
    }

    // ---------------------------------------------------------------------
    // patchEvent — authorization (C2)
    // ---------------------------------------------------------------------

    @Test
    void patchEvent_deniesWhenCallerBelongsToAnotherCompany() {
        UUID eventId = UUID.randomUUID();
        User creator = user(UUID.randomUUID(), COMPANY_A);
        Event event = event(eventId, creator);

        when(eventRepository.findByIdWithParticipants(eventId)).thenReturn(Optional.of(event));

        UUID outsider = UUID.randomUUID();
        assertThatThrownBy(() ->
                eventService.patchEvent(patchDto(eventId, null), outsider, COMPANY_B, Set.of("ROLE_MANAGER")))
                .isInstanceOf(AuthorizationDeniedException.class);

        verify(eventRepository, never()).save(any());
    }

    @Test
    void patchEvent_deniesWhenCallerIsNeitherCreatorNorManager() {
        UUID eventId = UUID.randomUUID();
        User creator = user(UUID.randomUUID(), COMPANY_A);
        Event event = event(eventId, creator);

        when(eventRepository.findByIdWithParticipants(eventId)).thenReturn(Optional.of(event));

        UUID colleague = UUID.randomUUID();
        assertThatThrownBy(() ->
                eventService.patchEvent(patchDto(eventId, null), colleague, COMPANY_A, Set.of("ROLE_USER")))
                .isInstanceOf(AuthorizationDeniedException.class);

        verify(eventRepository, never()).save(any());
    }

    @Test
    void patchEvent_allowsTheCreator() {
        UUID eventId = UUID.randomUUID();
        User creator = user(UUID.randomUUID(), COMPANY_A);
        Event event = event(eventId, creator);

        when(eventRepository.findByIdWithParticipants(eventId)).thenReturn(Optional.of(event));
        when(stringUtils.normalizeString("Riunione")).thenReturn("Riunione");
        when(eventTypeRepository.getReferenceById((short) 2)).thenReturn(new EventTypeEntity((short) 2, "Private"));
        when(eventRepository.save(event)).thenReturn(event);

        UUID result = eventService.patchEvent(
                patchDto(eventId, null), creator.getUserId(), COMPANY_A, Set.of("ROLE_USER"));

        assertThat(result).isEqualTo(eventId);
        verify(eventRepository).save(event);
    }

    @Test
    void patchEvent_allowsAManagerFromTheSameCompany() {
        UUID eventId = UUID.randomUUID();
        User creator = user(UUID.randomUUID(), COMPANY_A);
        Event event = event(eventId, creator);

        when(eventRepository.findByIdWithParticipants(eventId)).thenReturn(Optional.of(event));
        when(stringUtils.normalizeString("Riunione")).thenReturn("Riunione");
        when(eventTypeRepository.getReferenceById((short) 2)).thenReturn(new EventTypeEntity((short) 2, "Private"));
        when(eventRepository.save(event)).thenReturn(event);

        UUID result = eventService.patchEvent(
                patchDto(eventId, null), UUID.randomUUID(), COMPANY_A, Set.of("ROLE_MANAGER"));

        assertThat(result).isEqualTo(eventId);
        verify(eventRepository).save(event);
    }

    @Test
    void patchEvent_deniesAddingAParticipantFromAnotherCompany() {
        UUID eventId = UUID.randomUUID();
        User creator = user(UUID.randomUUID(), COMPANY_A);
        Event event = event(eventId, creator);
        User foreign = user(UUID.randomUUID(), COMPANY_B);

        when(eventRepository.findByIdWithParticipants(eventId)).thenReturn(Optional.of(event));
        when(stringUtils.normalizeString("Riunione")).thenReturn("Riunione");
        when(eventTypeRepository.getReferenceById((short) 2)).thenReturn(new EventTypeEntity((short) 2, "Private"));
        when(userService.getUsersByIds(Set.of(foreign.getUserId()))).thenReturn(List.of(foreign));

        assertThatThrownBy(() -> eventService.patchEvent(
                patchDto(eventId, Set.of(foreign.getUserId())), creator.getUserId(), COMPANY_A, Set.of("ROLE_USER")))
                .isInstanceOf(AuthorizationDeniedException.class);

        verify(eventRepository, never()).save(any());
    }

    @Test
    void patchEvent_reactivatesPreviouslySoftDeletedLink_insteadOfCreatingDuplicate() {
        UUID eventId = UUID.randomUUID();
        User creator = user(UUID.randomUUID(), COMPANY_A);
        Event event = event(eventId, creator);

        User participant = user(UUID.randomUUID(), COMPANY_A);
        EventUsers softDeletedLink = new EventUsers(participant, event);
        softDeletedLink.markDeleted(participant);

        when(eventRepository.findByIdWithParticipants(eventId)).thenReturn(Optional.of(event));
        when(eventTypeRepository.getReferenceById((short) 2)).thenReturn(new EventTypeEntity((short) 2, "Private"));
        when(userService.getUsersByIds(Set.of(participant.getUserId()))).thenReturn(List.of(participant));
        when(eventUsersRepository.findByIdIncludingDeleted(participant.getUserId(), eventId))
                .thenReturn(Optional.of(softDeletedLink));
        when(stringUtils.normalizeString("Riunione")).thenReturn("Riunione");
        when(eventRepository.save(event)).thenReturn(event);

        eventService.patchEvent(
                patchDto(eventId, Set.of(participant.getUserId())), creator.getUserId(), COMPANY_A, Set.of("ROLE_USER"));

        assertThat(softDeletedLink.isActive()).isTrue();
        assertThat(softDeletedLink.getDeletedAt()).isNull();
        assertThat(softDeletedLink.getDeletedBy()).isNull();
        assertThat(event.getUsers()).containsExactly(softDeletedLink);
    }

    // ---------------------------------------------------------------------
    // deleteEvent — authorization (C2)
    // ---------------------------------------------------------------------

    @Test
    void deleteEvent_deniesCrossTenantCaller() {
        UUID eventId = UUID.randomUUID();
        User creator = user(UUID.randomUUID(), COMPANY_A);
        Event event = event(eventId, creator);

        when(eventRepository.findByIdWithParticipants(eventId)).thenReturn(Optional.of(event));

        assertThatThrownBy(() ->
                eventService.deleteEvent(eventId, UUID.randomUUID(), COMPANY_B, Set.of("ROLE_MANAGER")))
                .isInstanceOf(AuthorizationDeniedException.class);

        verify(eventRepository, never()).delete(any());
    }

    @Test
    void deleteEvent_deniesCallerWhoIsNeitherCreatorNorManager() {
        UUID eventId = UUID.randomUUID();
        User creator = user(UUID.randomUUID(), COMPANY_A);
        Event event = event(eventId, creator);

        when(eventRepository.findByIdWithParticipants(eventId)).thenReturn(Optional.of(event));

        assertThatThrownBy(() ->
                eventService.deleteEvent(eventId, UUID.randomUUID(), COMPANY_A, Set.of("ROLE_USER")))
                .isInstanceOf(AuthorizationDeniedException.class);

        verify(eventRepository, never()).delete(any());
    }

    @Test
    void deleteEvent_allowsTheCreator() {
        UUID eventId = UUID.randomUUID();
        User creator = user(UUID.randomUUID(), COMPANY_A);
        Event event = event(eventId, creator);

        when(eventRepository.findByIdWithParticipants(eventId)).thenReturn(Optional.of(event));
        when(userService.getUserById(creator.getUserId())).thenReturn(creator);
        when(eventRepository.save(event)).thenReturn(event);

        eventService.deleteEvent(eventId, creator.getUserId(), COMPANY_A, Set.of("ROLE_USER"));

        verify(eventRepository).delete(event);
    }

    @Test
    void deleteEvent_allowsAManagerFromTheSameCompany() {
        UUID eventId = UUID.randomUUID();
        User creator = user(UUID.randomUUID(), COMPANY_A);
        Event event = event(eventId, creator);
        UUID manager = UUID.randomUUID();
        User managerUser = user(manager, COMPANY_A);

        when(eventRepository.findByIdWithParticipants(eventId)).thenReturn(Optional.of(event));
        when(userService.getUserById(manager)).thenReturn(managerUser);
        when(eventRepository.save(event)).thenReturn(event);

        eventService.deleteEvent(eventId, manager, COMPANY_A, Set.of("ROLE_MANAGER"));

        verify(eventRepository).delete(event);
    }
}
