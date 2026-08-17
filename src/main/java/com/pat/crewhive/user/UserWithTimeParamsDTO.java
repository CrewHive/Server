package com.pat.crewhive.user;

import java.math.BigDecimal;
import java.util.UUID;

public record UserWithTimeParamsDTO(
        UUID userId,
        String firstName,
        String lastName,
        String email,
        String companyName,
        ContractType contractType,
        int workableHoursPerWeek,
        BigDecimal overtimeHours,
        BigDecimal vacationDaysAccumulated,
        BigDecimal vacationDaysTaken,
        BigDecimal leaveDaysAccumulated,
        BigDecimal leaveDaysTaken
) {
}
