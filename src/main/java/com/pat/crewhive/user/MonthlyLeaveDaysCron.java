package com.pat.crewhive.user;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class MonthlyLeaveDaysCron {

    private static final Logger log = LoggerFactory.getLogger(MonthlyLeaveDaysCron.class);

    private final UserRepository userRepository;

    public MonthlyLeaveDaysCron(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Transactional
    @Scheduled(cron = "0 0 2 1 * *", zone = "Europe/Rome")
    public void run() {
        int updated = userRepository.accrueMonthlyLeaveDays();
        log.info("MonthlyVacationCron eseguito: {} utenti aggiornati.", updated);
    }
}

