package com.pat.crewhive.model.user.util;

import com.pat.crewhive.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor
public class MonthlyLeaveDaysCron {

    private final UserRepository userRepository;

    @Transactional
    @Scheduled(cron = "0 0 3 1 * *", zone = "Europe/Rome")
    public void run() {
        int updated = userRepository.accrueMonthlyLeaveDays();
        log.info("MonthlyVacationCron eseguito: {} utenti aggiornati.", updated);
    }
}

