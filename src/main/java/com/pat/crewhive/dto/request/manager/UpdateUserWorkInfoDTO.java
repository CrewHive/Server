package com.pat.crewhive.dto.request.manager;

import com.pat.crewhive.model.util.ContractType;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class UpdateUserWorkInfoDTO {

    @NotNull(message = "Target user ID is required")
    @Positive(message = "Target user ID must be a positive number")
    Long targetUserId;

    @NotNull(message = "Contract type is required")
    ContractType contractType;

    @Min(value = 0, message = "Workable hours per week must be at least 0")
    int workableHoursPerWeek;

    @Min(value = 0, message = "Overtime hours must be at least 0")
    BigDecimal overtimeHours;

    @Min(value = 0, message = "Vacation days accumulated must be at least 0")
    @Digits(fraction = 2, integer = 3, message = "Vacation days accumulated must be a number with up to 3 digits and 2 decimal places")
    BigDecimal vacationDaysAccumulated;

    @Min(value = 0, message = "Vacation days taken must be at least 0")
    @Digits(fraction = 2, integer = 3, message = "Vacation days taken must be a number with up to 3 digits and 2 decimal places")
    BigDecimal vacationDaysTaken;

    @Min(value = 0, message = "Leave days accumulated must be at least 0")
    @Digits(fraction = 2, integer = 3, message = "Leave days accumulated must be a number with up to 3 digits and 2 decimal places")
    BigDecimal leaveDaysAccumulated;

    @Min(value = 0, message = "Leave days taken must be at least 0")
    @Digits(fraction = 2, integer = 3, message = "Leave days taken must be a number with up to 3 digits and 2 decimal places")
    BigDecimal leaveDaysTaken;
}
