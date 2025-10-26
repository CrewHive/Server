package com.pat.crewhive.dto.response.company;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UserIdAndNameAndHoursDTO {

    private Long userId;

    private String firstName;

    private String lastName;

    private int workableHoursPerWeek;
}
