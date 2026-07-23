package com.pat.crewhive.company;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UserIdAndNameAndHoursDTO {

    private UUID userId;

    private String firstName;

    private String lastName;

    private int workableHoursPerWeek;
}
