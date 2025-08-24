package com.pat.crewhive.dto.user;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UserWithTimeParamsDTO {

    private Long userId;

    private String email;

    private String companyName;

    private int workableHoursPerWeek;

    private int overtimeHours;

    private BigDecimal vacationDaysAccumulated;

    private BigDecimal vacationDaysTaken;

    private BigDecimal leaveDaysAccumulated;

    private BigDecimal leaveDaysTaken;
}
