package com.pat.crewhive.event;

import com.pat.crewhive.user.User;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link Event}: participants management on both sides, date sync and accessors.
 */
class EventEntityTest {

    private static final OffsetDateTime START = OffsetDateTime.parse("2026-08-21T09:00:00+02:00");
    private static final OffsetDateTime END = START.plusHours(2);

    private User user() {
        User u = new User(UUID.randomUUID() + "@example.com", "Mario", "Rossi", "pwd");
        ReflectionTestUtils.setField(u, "userId", UUID.randomUUID());
        return u;
    }

    private Event event(User... participants) {
        return new Event(Set.of(participants), "Meeting", "desc", START, END, "FF0000", new EventTypeEntity((short) 2, "PRIVATE"));
    }

    @Test
    void constructor_setsTheFieldsAndAddsTheParticipantsAsAccepted() {
        User a = user();
        User b = user();

        Event event = event(a, b);

        assertThat(event.getEventName()).isEqualTo("Meeting");
        assertThat(event.getDescription()).isEqualTo("desc");
        assertThat(event.getStart()).isEqualTo(START);
        assertThat(event.getEnd()).isEqualTo(END);
        assertThat(event.getColor()).isEqualTo("FF0000");
        assertThat(event.getEventType().getName()).isEqualTo("PRIVATE");
        assertThat(event.getUsers()).hasSize(2).allMatch(eu -> eu.getStatus() == EventParticipationStatus.ACCEPTED);
        assertThat(a.getPersonalEvents()).hasSize(1);
        assertThat(event.getEventId()).isNull();
        assertThat(event.getVersion()).isNull();
    }

    @Test
    void constructor_derivesTheDateFromTheStart() {
        assertThat(event().getDate()).isEqualTo(LocalDate.of(2026, 8, 21));
    }

    @Test
    void syncDate_onPersistOrUpdate_followsTheStart() {
        Event event = event();
        event.setStart(OffsetDateTime.parse("2026-09-02T23:30:00+02:00"));

        ReflectionTestUtils.invokeMethod(event, "syncDate");

        assertThat(event.getDate()).isEqualTo(LocalDate.of(2026, 9, 2));
    }

    @Test
    void addUser_withStatus_linksBothSides_andIgnoresADuplicate() {
        Event event = event();
        User u = user();

        event.addUser(u, EventParticipationStatus.PENDING);
        event.addUser(u, EventParticipationStatus.ACCEPTED);

        assertThat(event.getUsers()).hasSize(1);
        assertThat(event.getUsers().iterator().next().getStatus()).isEqualTo(EventParticipationStatus.PENDING);
        assertThat(u.getPersonalEvents()).hasSize(1);
    }

    @Test
    void attachUser_addsAnAlreadyBuiltLinkToBothSides() {
        Event event = event();
        User u = user();
        EventUsers link = new EventUsers(u, event, EventParticipationStatus.DECLINED);

        event.attachUser(link);

        assertThat(event.getUsers()).containsExactly(link);
        assertThat(u.getPersonalEvents()).containsExactly(link);
    }

    @Test
    void removeUser_unlinksBothSides_andKeepsTheOthers() {
        User keep = user();
        User drop = user();
        Event event = event(keep, drop);
        EventUsers dropped = event.getUsers().stream().filter(eu -> eu.getUser() == drop).findFirst().orElseThrow();

        event.removeUser(drop);

        assertThat(event.getUsers()).extracting(EventUsers::getUser).containsExactly(keep);
        assertThat(drop.getPersonalEvents()).isEmpty();
        assertThat(dropped.getUser()).isNull();
        assertThat(dropped.getEvent()).isNull();
    }

    @Test
    void removeUser_ofANonParticipant_changesNothing() {
        User member = user();
        Event event = event(member);

        event.removeUser(user());

        assertThat(event.getUsers()).hasSize(1);
    }

    @Test
    void settersReplaceTheValues() {
        Event event = new Event();
        User creator = user();
        EventTypeEntity type = new EventTypeEntity((short) 1, "PUBLIC");
        Set<EventUsers> users = new HashSet<>();

        event.setEventName("n");
        event.setDescription("d");
        event.setStart(START);
        event.setEnd(END);
        event.setDate(LocalDate.of(2026, 1, 1));
        event.setColor("00FF00");
        event.setCreator(creator);
        event.setEventType(type);
        event.setUsers(users);

        assertThat(event.getEventName()).isEqualTo("n");
        assertThat(event.getDescription()).isEqualTo("d");
        assertThat(event.getStart()).isEqualTo(START);
        assertThat(event.getEnd()).isEqualTo(END);
        assertThat(event.getDate()).isEqualTo(LocalDate.of(2026, 1, 1));
        assertThat(event.getColor()).isEqualTo("00FF00");
        assertThat(event.getCreator()).isSameAs(creator);
        assertThat(event.getEventType()).isSameAs(type);
        assertThat(event.getUsers()).isSameAs(users);
    }

    @Test
    void eventTypeEnum_exposesIdAndLabel() {
        assertThat(EventType.PUBLIC.getId()).isEqualTo((short) 1);
        assertThat(EventType.PUBLIC.getLabel()).isEqualTo("Public");
        assertThat(EventType.PRIVATE.getId()).isEqualTo((short) 2);
        assertThat(EventType.PRIVATE.getLabel()).isEqualTo("Private");
    }

    @Test
    void eventTypeEntity_exposesIdAndName() {
        EventTypeEntity type = new EventTypeEntity((short) 2, "PRIVATE");

        assertThat(type.getId()).isEqualTo((short) 2);
        assertThat(type.getName()).isEqualTo("PRIVATE");
    }
}
