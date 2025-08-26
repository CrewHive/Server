package com.pat.crewhive.dto.shift.shift_programmed;

import com.pat.crewhive.model.util.EventType;
import com.pat.crewhive.security.sanitizer.annotation.NoHtml;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.OffsetDateTime;
import java.util.Set;


@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CreateShiftProgrammedDTO {

    @NotBlank(message = "Shift name cannot be blank")
    @NoHtml
    @Size(min = 3, max = 32)
    String name;

    @NoHtml
    @Size(max = 256)
    String description;

    @NotNull(message = "Shift start time cannot be null")
    OffsetDateTime start;

    @NotNull(message = "Shift end time cannot be null")
    OffsetDateTime end;

    @NotBlank(message = "Shift color cannot be blank")
    @NoHtml
    @Size(min = 6, max = 6)
    String color;

    Set<Long> userId;
}
