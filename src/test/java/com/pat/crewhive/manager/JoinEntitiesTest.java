package com.pat.crewhive.manager;

import com.pat.crewhive.event.Event;
import com.pat.crewhive.event.EventParticipationStatus;
import com.pat.crewhive.event.EventUsers;
import com.pat.crewhive.event.EventUsersId;
import com.pat.crewhive.shiftprogrammed.ShiftProgrammed;
import com.pat.crewhive.shiftprogrammed.ShiftUser;
import com.pat.crewhive.shiftprogrammed.ShiftUserId;
import com.pat.crewhive.user.User;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for the join entities {@link UserRole}, {@link ShiftUser} and {@link EventUsers}: accessors and
 * equality. A link whose id is not populated yet (before the flush) equals only itself, so that two freshly
 * built links never collapse in a {@code HashSet}.
 */
class JoinEntitiesTest {

    private User user() {
        User u = new User("u@example.com", "Mario", "Rossi", "pwd");
        ReflectionTestUtils.setField(u, "userId", UUID.randomUUID());
        return u;
    }

    // ---- UserRole ----

    @Test
    void userRole_keepsUserAndRole_andSettersReplaceThem() {
        User u = user();
        Role r = new Role("ROLE_USER");
        UserRole link = new UserRole(u, r);

        assertThat(link.getUser()).isSameAs(u);
        assertThat(link.getRole()).isSameAs(r);

        User other = user();
        Role otherRole = new Role("ROLE_X");
        link.setUser(other);
        link.setRole(otherRole);
        UserRoleId id = new UserRoleId(other.getUserId(), 3L);
        link.setId(id);

        assertThat(link.getUser()).isSameAs(other);
        assertThat(link.getRole()).isSameAs(otherRole);
        assertThat(link.getId()).isSameAs(id);
    }

    @Test
    void userRole_withUnpopulatedId_equalsOnlyItself_andBothStayInAHashSet() {
        UserRole a = new UserRole(user(), new Role("ROLE_A"));
        UserRole b = new UserRole(user(), new Role("ROLE_B"));

        assertThat(a).isEqualTo(a);
        assertThat(a).isNotEqualTo(b);
        assertThat(a).isNotEqualTo(null);
        assertThat(a).isNotEqualTo("x");
        assertThat(new HashSet<>(Set.of(a, b))).hasSize(2);
    }

    @Test
    void userRole_withPopulatedIds_equalsWhenTheIdsAreEqual() {
        UUID userId = UUID.randomUUID();
        UserRole a = new UserRole();
        a.setId(new UserRoleId(userId, 1L));
        UserRole b = new UserRole();
        b.setId(new UserRoleId(userId, 1L));
        UserRole c = new UserRole();
        c.setId(new UserRoleId(userId, 2L));

        assertThat(a).isEqualTo(b).hasSameHashCodeAs(b);
        assertThat(a).isNotEqualTo(c);
    }

    @Test
    void userRole_withOneSidePopulatedAndTheOtherNot_isNotEqual() {
        UserRole populated = new UserRole();
        populated.setId(new UserRoleId(UUID.randomUUID(), 1L));
        UserRole blank = new UserRole();

        assertThat(populated).isNotEqualTo(blank);
        assertThat(blank).isNotEqualTo(populated);
    }

    // ---- ShiftUser ----

    @Test
    void shiftUser_keepsShiftAndUser_andSettersReplaceThem() {
        ShiftProgrammed shift = new ShiftProgrammed();
        User u = user();
        ShiftUser link = new ShiftUser(shift, u);

        assertThat(link.getShift()).isSameAs(shift);
        assertThat(link.getUser()).isSameAs(u);

        ShiftProgrammed other = new ShiftProgrammed();
        User otherUser = user();
        ShiftUserId id = new ShiftUserId(UUID.randomUUID(), otherUser.getUserId());
        link.setShift(other);
        link.setUser(otherUser);
        link.setId(id);

        assertThat(link.getShift()).isSameAs(other);
        assertThat(link.getUser()).isSameAs(otherUser);
        assertThat(link.getId()).isSameAs(id);
    }

    @Test
    void shiftUser_withUnpopulatedId_equalsOnlyItself_andBothStayInAHashSet() {
        ShiftUser a = new ShiftUser(new ShiftProgrammed(), user());
        ShiftUser b = new ShiftUser(new ShiftProgrammed(), user());

        assertThat(a).isEqualTo(a);
        assertThat(a).isNotEqualTo(b);
        assertThat(a).isNotEqualTo(null);
        assertThat(a).isNotEqualTo("x");
        assertThat(new HashSet<>(Set.of(a, b))).hasSize(2);
    }

    @Test
    void shiftUser_withPopulatedIds_equalsWhenTheIdsAreEqual() {
        UUID shiftId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        ShiftUser a = new ShiftUser();
        a.setId(new ShiftUserId(shiftId, userId));
        ShiftUser b = new ShiftUser();
        b.setId(new ShiftUserId(shiftId, userId));
        ShiftUser c = new ShiftUser();
        c.setId(new ShiftUserId(shiftId, UUID.randomUUID()));
        ShiftUser blank = new ShiftUser();

        assertThat(a).isEqualTo(b).hasSameHashCodeAs(b);
        assertThat(a).isNotEqualTo(c);
        assertThat(a).isNotEqualTo(blank);
        assertThat(blank).isNotEqualTo(a);
    }

    // ---- EventUsers ----

    @Test
    void eventUsers_defaultsToAccepted_andTheConstructorCanSetAnotherStatus() {
        User u = user();
        Event e = new Event();

        assertThat(new EventUsers(u, e).getStatus()).isEqualTo(EventParticipationStatus.ACCEPTED);
        assertThat(new EventUsers(u, e, EventParticipationStatus.PENDING).getStatus()).isEqualTo(EventParticipationStatus.PENDING);
    }

    @Test
    void eventUsers_keepsEventAndUser_andSettersReplaceThem() {
        User u = user();
        Event e = new Event();
        EventUsers link = new EventUsers(u, e);

        assertThat(link.getUser()).isSameAs(u);
        assertThat(link.getEvent()).isSameAs(e);

        User otherUser = user();
        Event otherEvent = new Event();
        EventUsersId id = new EventUsersId(otherUser.getUserId(), UUID.randomUUID());
        link.setUser(otherUser);
        link.setEvent(otherEvent);
        link.setId(id);
        link.setStatus(EventParticipationStatus.DECLINED);

        assertThat(link.getUser()).isSameAs(otherUser);
        assertThat(link.getEvent()).isSameAs(otherEvent);
        assertThat(link.getId()).isSameAs(id);
        assertThat(link.getStatus()).isEqualTo(EventParticipationStatus.DECLINED);
    }

    @Test
    void eventUsers_withUnpopulatedId_equalsOnlyItself_andBothStayInAHashSet() {
        EventUsers a = new EventUsers(user(), new Event());
        EventUsers b = new EventUsers(user(), new Event());

        assertThat(a).isEqualTo(a);
        assertThat(a).isNotEqualTo(b);
        assertThat(a).isNotEqualTo(null);
        assertThat(a).isNotEqualTo("x");
        assertThat(new HashSet<>(Set.of(a, b))).hasSize(2);
    }

    @Test
    void eventUsers_withPopulatedIds_equalsWhenTheIdsAreEqual() {
        UUID userId = UUID.randomUUID();
        UUID eventId = UUID.randomUUID();
        EventUsers a = new EventUsers();
        a.setId(new EventUsersId(userId, eventId));
        EventUsers b = new EventUsers();
        b.setId(new EventUsersId(userId, eventId));
        EventUsers c = new EventUsers();
        c.setId(new EventUsersId(userId, UUID.randomUUID()));
        EventUsers blank = new EventUsers();

        assertThat(a).isEqualTo(b).hasSameHashCodeAs(b);
        assertThat(a).isNotEqualTo(c);
        assertThat(a).isNotEqualTo(blank);
        assertThat(blank).isNotEqualTo(a);
    }
}
