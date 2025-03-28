package com.pat.hours_calculator.model.json;

import lombok.Getter;
import lombok.Setter;

import java.sql.Timestamp;

@Getter
@Setter
public class Contract {

    private Long id;
    private String name;
    private String description;
    private String startDate;
    private String endDate;
    private String hoursPerWeek;
    private String hoursPerDay;
    private String daysPerWeek;
    private String startTime;
    private String endTime;
    private Timestamp breakTime;

}
