package com.pat.crewhive.common;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link DateUtils}. The class reads the system clock, so expectations are computed
 * from {@code LocalDate.now()} as well (a run across midnight could in theory flake).
 * TRIMESTER, SEMESTER and YEAR are windows around today (-1/+1, -2/+2, -5/+5 months): these tests
 * pin the current behaviour.
 */
class DateUtilsTest {

    private final DateUtils dateUtils = new DateUtils();

    @Test
    void day_isToday() {
        LocalDate today = LocalDate.now();

        assertThat(dateUtils.getStartDateForPeriod(Period.DAY)).isEqualTo(today);
        assertThat(dateUtils.getEndDateForPeriod(Period.DAY)).isEqualTo(today);
    }

    @Test
    void week_goesFromMondayToSunday() {
        LocalDate start = dateUtils.getStartDateForPeriod(Period.WEEK);
        LocalDate end = dateUtils.getEndDateForPeriod(Period.WEEK);

        assertThat(start.getDayOfWeek()).isEqualTo(DayOfWeek.MONDAY);
        assertThat(end.getDayOfWeek()).isEqualTo(DayOfWeek.SUNDAY);
        assertThat(start).isEqualTo(LocalDate.now().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)));
        assertThat(end).isEqualTo(start.plusDays(6));
    }

    @Test
    void month_goesFromFirstToLastDayOfCurrentMonth() {
        LocalDate now = LocalDate.now();

        assertThat(dateUtils.getStartDateForPeriod(Period.MONTH)).isEqualTo(now.withDayOfMonth(1));
        assertThat(dateUtils.getEndDateForPeriod(Period.MONTH)).isEqualTo(now.with(TemporalAdjusters.lastDayOfMonth()));
    }

    @Test
    void trimester_isCurrentMonthPlusOneBeforeAndOneAfter() {
        LocalDate now = LocalDate.now();

        assertThat(dateUtils.getStartDateForPeriod(Period.TRIMESTER)).isEqualTo(now.minusMonths(1).withDayOfMonth(1));
        assertThat(dateUtils.getEndDateForPeriod(Period.TRIMESTER))
                .isEqualTo(now.plusMonths(1).with(TemporalAdjusters.lastDayOfMonth()));
    }

    @Test
    void semester_isCurrentMonthPlusTwoBeforeAndTwoAfter() {
        LocalDate now = LocalDate.now();

        assertThat(dateUtils.getStartDateForPeriod(Period.SEMESTER)).isEqualTo(now.minusMonths(2).withDayOfMonth(1));
        assertThat(dateUtils.getEndDateForPeriod(Period.SEMESTER))
                .isEqualTo(now.plusMonths(2).with(TemporalAdjusters.lastDayOfMonth()));
    }

    @Test
    void year_isCurrentMonthPlusFiveBeforeAndFiveAfter() {
        LocalDate now = LocalDate.now();

        assertThat(dateUtils.getStartDateForPeriod(Period.YEAR)).isEqualTo(now.minusMonths(5).withDayOfMonth(1));
        assertThat(dateUtils.getEndDateForPeriod(Period.YEAR))
                .isEqualTo(now.plusMonths(5).with(TemporalAdjusters.lastDayOfMonth()));
    }

    @ParameterizedTest
    @EnumSource(Period.class)
    void everyPeriod_hasStartNotAfterEnd_andContainsToday(Period period) {
        LocalDate start = dateUtils.getStartDateForPeriod(period);
        LocalDate end = dateUtils.getEndDateForPeriod(period);
        LocalDate today = LocalDate.now();

        assertThat(start).isBeforeOrEqualTo(end);
        assertThat(today).isBetween(start, end);
    }
}
