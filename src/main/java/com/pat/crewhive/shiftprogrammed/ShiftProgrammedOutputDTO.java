package com.pat.crewhive.shiftprogrammed;

import java.util.List;

public record ShiftProgrammedOutputDTO(
        List<ShiftProgrammedItemDTO> shifts
) {
}
