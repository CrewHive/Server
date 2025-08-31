package com.pat.crewhive.dto.user;

import com.pat.crewhive.model.util.ContractType;
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

    private String username;

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
