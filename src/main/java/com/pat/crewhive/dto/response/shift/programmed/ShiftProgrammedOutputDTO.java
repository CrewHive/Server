package com.pat.crewhive.dto.response.shift.programmed;

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

    //todo ritorna un DTO con le info dello shift programmato e la lista di utenti assegnati a quello shift
    private List<ShiftProgrammed> shifts;

    private List<NameAndUserIdForShiftProgrammedDTO> users;
}
