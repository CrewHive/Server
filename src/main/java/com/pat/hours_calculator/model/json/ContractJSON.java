package com.pat.hours_calculator.model.json;

import jakarta.annotation.Nullable;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ContractJSON {

    private String startDate;
    private String endDate;
    private String hoursPerWeek;
    private boolean indefinite;
    private String startTime;
    @Nullable
    private String endTime;

}
