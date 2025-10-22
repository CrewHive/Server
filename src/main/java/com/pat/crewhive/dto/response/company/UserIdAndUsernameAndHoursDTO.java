package com.pat.crewhive.dto.response.company;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UserIdAndUsernameAndHoursDTO {

    private Long userId;

    private String username;

    private int workableHoursPerWeek;
}
