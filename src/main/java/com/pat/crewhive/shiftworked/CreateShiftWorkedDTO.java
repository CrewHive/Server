package com.pat.crewhive.shiftworked;


import com.pat.crewhive.security.sanitizer.annotation.NoHtml;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public record CreateShiftWorkedDTO(

        @NotBlank(message = "Shift name must not be blank")
        @NoHtml
        @Size(min = 3, max = 32, message = "Shift name must be between 3 and 32 characters")
        String shiftName,

        @NotNull(message = "Start time must not be null")
        OffsetDateTime start,

        @NotNull(message = "End time must not be null")
        OffsetDateTime end,

        @NotNull(message = "Shift date must not be null")
        int breakTime,

        @NotNull(message = "Total hours must not be null")
        BigDecimal extraHours,

        @NotNull(message = "User ID must not be null")
        UUID userId
) {
}
