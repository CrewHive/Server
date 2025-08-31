package com.pat.crewhive.service.utils;

import com.pat.crewhive.model.util.Period;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;

@Slf4j
@Component
public class DateUtils {

    public DateUtils() {
    }

    public LocalDate getStartDateForPeriod(Period period) {

        LocalDate now = LocalDate.now();

        return switch (period) {

            case DAY -> now;

            case WEEK -> now.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));

            case MONTH -> now.with(TemporalAdjusters.firstDayOfMonth());

            case TRIMESTER -> now.minusMonths(1).withDayOfMonth(1);

            case SEMESTER -> now.minusMonths(2).withDayOfMonth(1);

            case YEAR -> now.minusMonths(5).withDayOfMonth(1);
        };
    }

    public LocalDate getEndDateForPeriod(Period period) {

        LocalDate now = LocalDate.now();

         return switch (period) {

            case DAY -> now;

            case WEEK -> now.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY));

            case MONTH -> now.with(TemporalAdjusters.lastDayOfMonth());

            case TRIMESTER -> now.plusMonths(1).with(TemporalAdjusters.lastDayOfMonth());

            case SEMESTER -> now.plusMonths(2).with(TemporalAdjusters.lastDayOfMonth());

            case YEAR -> now.plusMonths(5).with(TemporalAdjusters.lastDayOfMonth());
        };
    }
}
