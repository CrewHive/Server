package com.pat.crewhive.user;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UserWithTimeParamsDTO {

    private UUID userId;

    private String firstName;

    private String lastName;

    private String email;

    private String companyName;

    private ContractType contractType;

    private int workableHoursPerWeek;

    private BigDecimal overtimeHours;

    private BigDecimal vacationDaysAccumulated;

    private BigDecimal vacationDaysTaken;

    private BigDecimal leaveDaysAccumulated;

    private BigDecimal leaveDaysTaken;
}
