package com.pat.crewhive.shiftworked;


import com.pat.crewhive.security.sanitizer.annotation.NoHtml;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

/**
 * Payload per registrare un turno lavorato dell'utente corrente.
 * L'utente target NON è nel body: viene sempre preso dal token (self-only).
 */
public record CreateShiftWorkedDTO(

        @NotBlank(message = "Shift name must not be blank")
        @NoHtml
        @Size(min = 3, max = 32, message = "Shift name must be between 3 and 32 characters")
        String shiftName,

        @NotNull(message = "Start time must not be null")
        OffsetDateTime start,

        @NotNull(message = "End time must not be null")
        OffsetDateTime end,

        @PositiveOrZero(message = "Break time must not be negative")
        int breakTime,

        @NotNull(message = "Extra hours must not be null")
        @PositiveOrZero(message = "Extra hours must not be negative")
        BigDecimal extraHours
) {

    @AssertTrue(message = "End time must be after start time")
    private boolean isChronologicallyValid() {
        return start == null || end == null || end.isAfter(start);
    }
}
