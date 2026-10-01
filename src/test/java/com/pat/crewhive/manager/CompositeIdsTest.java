package com.pat.crewhive.manager;

import com.pat.crewhive.event.EventUsersId;
import com.pat.crewhive.shiftprogrammed.ShiftUserId;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Value semantics of the composite ids: they are keys of the persistence context, so equals and hashCode
 * must be consistent and must not fail on a not yet populated id.
 */
class CompositeIdsTest {

    private static final UUID A = UUID.randomUUID();
    private static final UUID B = UUID.randomUUID();

    // ---- UserRoleId ----

    @Test
    void userRoleId_exposesItsParts_andSettersReplaceThem() {
        UserRoleId id = new UserRoleId(A, 7L);

        assertThat(id.getUserId()).isEqualTo(A);
        assertThat(id.getRoleId()).isEqualTo(7L);

        id.setUserId(B);
        id.setRoleId(8L);

        assertThat(id.getUserId()).isEqualTo(B);
        assertThat(id.getRoleId()).isEqualTo(8L);
    }

    @Test
    void userRoleId_equalsAndHashCode_followBothParts() {
        UserRoleId id = new UserRoleId(A, 7L);

        assertThat(id).isEqualTo(id);
        assertThat(id).isEqualTo(new UserRoleId(A, 7L)).hasSameHashCodeAs(new UserRoleId(A, 7L));
        assertThat(id).isNotEqualTo(new UserRoleId(B, 7L));
        assertThat(id).isNotEqualTo(new UserRoleId(A, 8L));
        assertThat(id).isNotEqualTo(null);
        assertThat(id).isNotEqualTo("not an id");
    }

    @Test
    void userRoleId_hashCodeOfAnEmptyId_doesNotFail() {
        assertThat(new UserRoleId().hashCode()).isEqualTo(new UserRoleId().hashCode());
        assertThat(new UserRoleId()).isEqualTo(new UserRoleId());
    }

    // ---- ShiftUserId ----

    @Test
    void shiftUserId_exposesItsParts_andSettersReplaceThem() {
        ShiftUserId id = new ShiftUserId(A, B);

        assertThat(id.getShiftProgrammedId()).isEqualTo(A);
        assertThat(id.getUserId()).isEqualTo(B);

        id.setShiftProgrammedId(B);
        id.setUserId(A);

        assertThat(id.getShiftProgrammedId()).isEqualTo(B);
        assertThat(id.getUserId()).isEqualTo(A);
    }

    @Test
    void shiftUserId_equalsAndHashCode_followBothParts() {
        ShiftUserId id = new ShiftUserId(A, B);

        assertThat(id).isEqualTo(id);
        assertThat(id).isEqualTo(new ShiftUserId(A, B)).hasSameHashCodeAs(new ShiftUserId(A, B));
        assertThat(id).isNotEqualTo(new ShiftUserId(B, B));
        assertThat(id).isNotEqualTo(new ShiftUserId(A, A));
        assertThat(id).isNotEqualTo(null);
        assertThat(id).isNotEqualTo("not an id");
        assertThat(new ShiftUserId().hashCode()).isEqualTo(new ShiftUserId().hashCode());
    }

    // ---- EventUsersId ----

    @Test
    void eventUsersId_exposesItsParts_andSettersReplaceThem() {
        EventUsersId id = new EventUsersId(A, B);

        assertThat(id.getUserId()).isEqualTo(A);
        assertThat(id.getEventId()).isEqualTo(B);

        id.setUserId(B);
        id.setEventId(A);

        assertThat(id.getUserId()).isEqualTo(B);
        assertThat(id.getEventId()).isEqualTo(A);
    }

    @Test
    void eventUsersId_equalsAndHashCode_followBothParts() {
        EventUsersId id = new EventUsersId(A, B);

        assertThat(id).isEqualTo(id);
        assertThat(id).isEqualTo(new EventUsersId(A, B)).hasSameHashCodeAs(new EventUsersId(A, B));
        assertThat(id).isNotEqualTo(new EventUsersId(B, B));
        assertThat(id).isNotEqualTo(new EventUsersId(A, A));
        assertThat(id).isNotEqualTo(null);
        assertThat(id).isNotEqualTo("not an id");
        assertThat(new EventUsersId().hashCode()).isEqualTo(new EventUsersId().hashCode());
    }
}
