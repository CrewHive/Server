package com.pat.hours_calculator.model.json;

import jakarta.annotation.Nullable;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
public class ContractJSON {

    private LocalDate startDate;
    @Nullable
    private LocalDate endDate;
    private int hoursPerWeek;
    private boolean indefinite;

}
