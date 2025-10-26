package com.pat.crewhive.dto.response.shift.programmed;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class NameAndUserIdForShiftProgrammedDTO {

    private List<String> firstName;

    private List<String> lastName;

    private List<Long> userId;

    private Long shiftProgrammedId;
}
