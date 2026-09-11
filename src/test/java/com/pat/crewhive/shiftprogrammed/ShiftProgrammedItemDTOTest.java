package com.pat.crewhive.shiftprogrammed;

import com.pat.crewhive.user.User;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link ShiftProgrammedItemDTO#from(ShiftProgrammed)}.
 */
class ShiftProgrammedItemDTOTest {

    private User buildUser(UUID userId, String firstName, String lastName) {
        User user = new User("user@example.com", firstName, lastName, "encoded-pwd");
        ReflectionTestUtils.setField(user, "userId", userId);
        return user;
    }

    @Test
    void from_mapsShiftFieldsAndUsers() {
        UUID shiftId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        OffsetDateTime start = OffsetDateTime.parse("2026-08-21T09:00:00Z");
        OffsetDateTime end = OffsetDateTime.parse("2026-08-21T17:00:00Z");

        ShiftProgrammed shift = new ShiftProgrammed();
        ReflectionTestUtils.setField(shift, "id", shiftId);
        shift.setShiftName("Turno mattina");
        shift.setDescription("Apertura negozio");
        shift.setStart(start);
        shift.setEnd(end);
        ReflectionTestUtils.setField(shift, "date", LocalDate.of(2026, 8, 21));
        shift.setColor("#0000FF");

        User user = buildUser(userId, "Luigi", "Verdi");
        shift.addUser(user);

        ShiftProgrammedItemDTO dto = ShiftProgrammedItemDTO.from(shift);

        assertThat(dto.shiftProgrammedId()).isEqualTo(shiftId);
        assertThat(dto.shiftName()).isEqualTo("Turno mattina");
        assertThat(dto.description()).isEqualTo("Apertura negozio");
        assertThat(dto.start()).isEqualTo(start);
        assertThat(dto.end()).isEqualTo(end);
        assertThat(dto.date()).isEqualTo(LocalDate.of(2026, 8, 21));
        assertThat(dto.color()).isEqualTo("#0000FF");
        assertThat(dto.users())
                .containsExactly(new ShiftParticipantDTO(userId, "Luigi", "Verdi"));
    }

    @Test
    void from_shiftWithoutUsers_returnsEmptyUsersList() {
        ShiftProgrammed shift = new ShiftProgrammed();
        ReflectionTestUtils.setField(shift, "id", UUID.randomUUID());
        shift.setShiftName("Turno vuoto");
        shift.setStart(OffsetDateTime.now());
        shift.setEnd(OffsetDateTime.now().plusHours(1));
        shift.setColor("#123456");

        ShiftProgrammedItemDTO dto = ShiftProgrammedItemDTO.from(shift);

        assertThat(dto.users()).isEmpty();
    }
}
