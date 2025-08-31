package com.pat.crewhive.dto.shift.shift_programmed;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UsernameAndUserIdForShiftProgrammedDTO {

    private List<String> username;

    private List<Long> userId;

    private Long shiftProgrammedId;
}
