package com.pat.crewhive.shiftprogrammed;

import java.util.List;
import java.util.UUID;

public record NameAndUserIdForShiftProgrammedDTO(
        List<String> firstName,
        List<String> lastName,
        List<UUID> userId,
        UUID shiftProgrammedId
) {
}
