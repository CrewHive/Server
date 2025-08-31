package com.pat.crewhive.dto.shift.shift_programmed;

import com.pat.crewhive.model.shift.shiftprogrammed.entity.ShiftProgrammed;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ShiftProgrammedOutputDTO {

    private List<ShiftProgrammed> shifts;

    private List<UsernameAndUserIdForShiftProgrammedDTO> usernames;
}
