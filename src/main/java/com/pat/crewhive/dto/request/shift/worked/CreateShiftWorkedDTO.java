package com.pat.crewhive.dto.request.shift.worked;


import com.pat.crewhive.security.sanitizer.annotation.NoHtml;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CreateShiftWorkedDTO {

    @NotBlank(message = "Shift name must not be blank")
    @NoHtml
    @Size(min = 3, max = 32, message = "Shift name must be between 3 and 32 characters")
    private String shiftName;

    @NotNull(message = "Start time must not be null")
    private OffsetDateTime start;

    @NotNull(message = "End time must not be null")
    private OffsetDateTime end;

    @NotNull(message = "Shift date must not be null")
    private int breakTime;

    @NotNull(message = "Total hours must not be null")
    private BigDecimal extraHours;

    @NotNull(message = "User ID must not be null")
    private Long userId;

}
