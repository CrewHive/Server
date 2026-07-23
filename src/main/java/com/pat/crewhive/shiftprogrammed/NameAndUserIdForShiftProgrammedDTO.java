package com.pat.crewhive.shiftprogrammed;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class NameAndUserIdForShiftProgrammedDTO {

    private List<String> firstName;

    private List<String> lastName;

    private List<UUID> userId;

    private UUID shiftProgrammedId;
}
