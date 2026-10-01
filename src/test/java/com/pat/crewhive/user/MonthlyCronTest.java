package com.pat.crewhive.user;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.scheduling.annotation.Scheduled;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link MonthlyLeaveDaysCron} and {@link MonthlyVacationCron}: each delegates to its own
 * repository accrual query, and both are scheduled at 02:00 of the 1st of the month (Europe/Rome).
 */
@ExtendWith(MockitoExtension.class)
class MonthlyCronTest {

    @Mock
    private UserRepository userRepository;

    @Test
    void leaveDaysCron_runsOnlyTheLeaveDaysAccrual() {
        when(userRepository.accrueMonthlyLeaveDays()).thenReturn(3);

        new MonthlyLeaveDaysCron(userRepository).run();

        verify(userRepository).accrueMonthlyLeaveDays();
        verifyNoMoreInteractions(userRepository);
    }

    @Test
    void vacationCron_runsOnlyTheVacationAccrual() {
        when(userRepository.accrueMonthlyVacationDays()).thenReturn(5);

        new MonthlyVacationCron(userRepository).run();

        verify(userRepository).accrueMonthlyVacationDays();
        verifyNoMoreInteractions(userRepository);
    }

    @Test
    void bothCrons_areScheduledOnTheFirstOfTheMonthAtTwoRomeTime() throws NoSuchMethodException {
        Scheduled leave = MonthlyLeaveDaysCron.class.getMethod("run").getAnnotation(Scheduled.class);
        Scheduled vacation = MonthlyVacationCron.class.getMethod("run").getAnnotation(Scheduled.class);

        assertThat(leave.cron()).isEqualTo("0 0 2 1 * *");
        assertThat(leave.zone()).isEqualTo("Europe/Rome");
        assertThat(vacation.cron()).isEqualTo("0 0 2 1 * *");
        assertThat(vacation.zone()).isEqualTo("Europe/Rome");
    }
}
