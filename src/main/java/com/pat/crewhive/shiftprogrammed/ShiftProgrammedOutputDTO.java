package com.pat.crewhive.shiftprogrammed;

import java.util.List;

//todo ritorna un DTO con le info dello shift programmato e la lista di utenti assegnati a quello shift
public record ShiftProgrammedOutputDTO(
        List<ShiftProgrammed> shifts,
        List<NameAndUserIdForShiftProgrammedDTO> users
) {
}
