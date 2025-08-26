package com.pat.crewhive.service.utils;

import com.pat.crewhive.model.util.Period;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.List;

@Slf4j
@Component
@NoArgsConstructor
@AllArgsConstructor
public class DateUtils {

    public LocalDate getStartDateForPeriod(Period period) {

        LocalDate now = LocalDate.now();
        LocalDate from;

        switch (period) {
            case DAY -> from = now;
            case WEEK -> from = now.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
            case MONTH -> from = now.with(TemporalAdjusters.firstDayOfMonth());
            case TRIMESTER -> {
                int q = ((now.getMonthValue() - 1) / 3) + 1;
                int startMonth = (q - 1) * 3 + 1;
                from = LocalDate.of(now.getYear(), startMonth, 1);
            }
            case SEMESTER -> {
                int startMonth = (now.getMonthValue() <= 6) ? 1 : 7;
                from = LocalDate.of(now.getYear(), startMonth, 1);
            }
            case YEAR -> {
                from = LocalDate.of(now.getYear(), 1, 1);
            }
            default -> {
                log.warn("Unknown Period: {}. Returning current date.", period);
                return null;
            }
        };

        return from;
    }

    public LocalDate getEndDateForPeriod(Period period) {

        LocalDate now = LocalDate.now();
        LocalDate to;

        switch (period) {
            case DAY -> to = now;
            case WEEK -> to = now.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY));
            case MONTH -> to = now.with(TemporalAdjusters.lastDayOfMonth());
            case TRIMESTER -> {
                int q = ((now.getMonthValue() - 1) / 3) + 1;
                int startMonth = (q - 1) * 3 + 1;
                LocalDate from = LocalDate.of(now.getYear(), startMonth, 1);
                to = from.plusMonths(3).minusDays(1);
            }
            case SEMESTER -> {
                int startMonth = (now.getMonthValue() <= 6) ? 1 : 7;
                LocalDate from = LocalDate.of(now.getYear(), startMonth, 1);
                to = from.plusMonths(6).minusDays(1);
            }
            case YEAR -> to = LocalDate.of(now.getYear(), 12, 31);
            default -> {
                log.warn("Unknown Period: {}. Returning current date.", period);
                return null;
            }
        };

        return to;
    }
}
