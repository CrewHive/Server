package com.pat.crewhive.shiftworked;

import com.pat.crewhive.company.Company;
import com.pat.crewhive.security.exception.custom.InvalidRequestException;
import com.pat.crewhive.user.User;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for {@link ShiftWorked}: worked hours computation and lifecycle callbacks.
 */
class ShiftWorkedEntityTest {

    private static final OffsetDateTime START = OffsetDateTime.parse("2026-08-21T09:00:00+02:00");

    private final User user = new User("m@example.com", "Mario", "Rossi", "pwd");
    private final Company company = new Company();

    private ShiftWorked shift(OffsetDateTime end, int breakMinutes, BigDecimal extra) {
        return new ShiftWorked("night", START, end, breakMinutes, extra, user, company);
    }

    @Test
    void workedHours_areTheDurationMinusTheBreak() {
        ShiftWorked shift = shift(START.plusHours(8), 30, new BigDecimal("1.50"));

        assertThat(shift.getWorkedHours()).isEqualByComparingTo("7.50");
        assertThat(shift.getBreakTime()).isEqualTo(30);
        assertThat(shift.getExtraHours()).isEqualByComparingTo("1.50");
    }

    @Test
    void workedHours_areRoundedToTwoDecimals() {
        ShiftWorked shift = shift(START.plusMinutes(100), 0, BigDecimal.ZERO);

        assertThat(shift.getWorkedHours()).isEqualByComparingTo("1.67");
    }

    @Test
    void aBreakLongerThanTheShift_givesZeroHours_notNegative() {
        ShiftWorked shift = shift(START.plusHours(1), 90, BigDecimal.ZERO);

        assertThat(shift.getWorkedHours()).isEqualByComparingTo("0");
    }

    @Test
    void nullExtraHours_becomeZero() {
        assertThat(shift(START.plusHours(1), 0, null).getExtraHours()).isEqualByComparingTo("0");
    }

    @Test
    void endNotAfterStart_isRejected() {
        assertThatThrownBy(() -> shift(START, 0, BigDecimal.ZERO)).isInstanceOf(InvalidRequestException.class);
        assertThatThrownBy(() -> shift(START.minusHours(1), 0, BigDecimal.ZERO)).isInstanceOf(InvalidRequestException.class);
    }

    @Test
    void constructor_setsTheCommonFields() {
        ShiftWorked shift = shift(START.plusHours(2), 0, BigDecimal.ZERO);

        assertThat(shift.getShiftName()).isEqualTo("night");
        assertThat(shift.getStart()).isEqualTo(START);
        assertThat(shift.getEnd()).isEqualTo(START.plusHours(2));
        assertThat(shift.getCompany()).isSameAs(company);
        assertThat(shift.getUser()).isSameAs(user);
        assertThat(shift.getShiftWorkedId()).isNull();
    }

    @Test
    void recomputeWorkedHours_onPersistOrUpdate_followsTheCurrentValues() {
        ShiftWorked shift = shift(START.plusHours(8), 0, BigDecimal.ZERO);
        shift.setEnd(START.plusHours(4));
        shift.setBreakTime(60);
        shift.setExtraHours(null);

        ReflectionTestUtils.invokeMethod(shift, "recomputeWorkedHours");

        assertThat(shift.getWorkedHours()).isEqualByComparingTo("3.00");
        assertThat(shift.getExtraHours()).isEqualByComparingTo("0");
    }

    @Test
    void syncDate_onPersistOrUpdate_followsTheStart() {
        ShiftWorked shift = shift(START.plusHours(1), 0, BigDecimal.ZERO);
        shift.setStart(OffsetDateTime.parse("2026-09-02T08:00:00+02:00"));

        ReflectionTestUtils.invokeMethod(shift, "syncDate");

        assertThat(shift.getDate()).isEqualTo(LocalDate.of(2026, 9, 2));
    }

    @Test
    void settersReplaceTheValues() {
        ShiftWorked shift = new ShiftWorked();
        User other = new User("o@example.com", "Luigi", "Verdi", "pwd");

        shift.setBreakTime(15);
        shift.setWorkedHours(new BigDecimal("6.00"));
        shift.setExtraHours(new BigDecimal("2.00"));
        shift.setUser(other);
        shift.setDate(LocalDate.of(2026, 1, 1));

        assertThat(shift.getBreakTime()).isEqualTo(15);
        assertThat(shift.getWorkedHours()).isEqualByComparingTo("6.00");
        assertThat(shift.getExtraHours()).isEqualByComparingTo("2.00");
        assertThat(shift.getUser()).isSameAs(other);
        assertThat(shift.getDate()).isEqualTo(LocalDate.of(2026, 1, 1));
    }
}
