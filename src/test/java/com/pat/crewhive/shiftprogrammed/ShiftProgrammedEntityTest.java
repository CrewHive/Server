package com.pat.crewhive.shiftprogrammed;

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
 * Unit tests for {@link ShiftProgrammed}: participants management on both sides and accessors.
 */
class ShiftProgrammedEntityTest {

    private User user() {
        User u = new User(UUID.randomUUID() + "@example.com", "Mario", "Rossi", "pwd");
        ReflectionTestUtils.setField(u, "userId", UUID.randomUUID());
        return u;
    }

    @Test
    void addUser_linksBothSides_andIgnoresADuplicate() {
        ShiftProgrammed shift = new ShiftProgrammed();
        User u = user();

        shift.addUser(u);
        shift.addUser(u);

        assertThat(shift.getUsers()).hasSize(1);
        assertThat(u.getShiftUsers()).hasSize(1);
    }

    @Test
    void attachUser_addsAnAlreadyBuiltLinkToBothSides() {
        ShiftProgrammed shift = new ShiftProgrammed();
        User u = user();
        ShiftUser link = new ShiftUser(shift, u);

        shift.attachUser(link);

        assertThat(shift.getUsers()).containsExactly(link);
        assertThat(u.getShiftUsers()).containsExactly(link);
    }

    @Test
    void removeUser_unlinksBothSides_andKeepsTheOthers() {
        ShiftProgrammed shift = new ShiftProgrammed();
        User keep = user();
        User drop = user();
        shift.addUser(keep);
        shift.addUser(drop);
        ShiftUser dropped = shift.getUsers().stream().filter(su -> su.getUser() == drop).findFirst().orElseThrow();

        shift.removeUser(drop);

        assertThat(shift.getUsers()).extracting(ShiftUser::getUser).containsExactly(keep);
        assertThat(drop.getShiftUsers()).isEmpty();
        assertThat(dropped.getUser()).isNull();
        assertThat(dropped.getShift()).isNull();
    }

    @Test
    void removeUser_ofANonParticipant_changesNothing() {
        ShiftProgrammed shift = new ShiftProgrammed();
        shift.addUser(user());

        shift.removeUser(user());

        assertThat(shift.getUsers()).hasSize(1);
    }

    @Test
    void settersReplaceTheValues() {
        ShiftProgrammed shift = new ShiftProgrammed();
        Set<ShiftUser> users = new HashSet<>();
        OffsetDateTime start = OffsetDateTime.parse("2026-08-21T09:00:00+02:00");

        shift.setDescription("desc");
        shift.setColor("0000FF");
        shift.setUsers(users);
        shift.setShiftName("morning");
        shift.setStart(start);
        shift.setEnd(start.plusHours(8));

        assertThat(shift.getDescription()).isEqualTo("desc");
        assertThat(shift.getColor()).isEqualTo("0000FF");
        assertThat(shift.getUsers()).isSameAs(users);
        assertThat(shift.getShiftName()).isEqualTo("morning");
        assertThat(shift.getShiftProgrammedId()).isNull();
        assertThat(shift.getVersion()).isNull();
    }

    @Test
    void syncDate_onPersistOrUpdate_followsTheStart() {
        ShiftProgrammed shift = new ShiftProgrammed();
        shift.setStart(OffsetDateTime.parse("2026-09-02T23:30:00+02:00"));

        ReflectionTestUtils.invokeMethod(shift, "syncDate");

        assertThat(shift.getDate()).isEqualTo(LocalDate.of(2026, 9, 2));
    }

    @Test
    void syncDate_withoutStart_keepsTheDate() {
        ShiftProgrammed shift = new ShiftProgrammed();
        shift.setDate(LocalDate.of(2026, 1, 1));

        ReflectionTestUtils.invokeMethod(shift, "syncDate");

        assertThat(shift.getDate()).isEqualTo(LocalDate.of(2026, 1, 1));
    }
}
