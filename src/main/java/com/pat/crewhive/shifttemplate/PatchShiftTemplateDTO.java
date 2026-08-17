package com.pat.crewhive.shifttemplate;

import com.pat.crewhive.security.sanitizer.annotation.NoHtml;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.OffsetTime;
import java.util.UUID;

public record PatchShiftTemplateDTO(

        @NotBlank(message = "Shift name must not be blank")
        @NoHtml
        @Size(min = 1, max = 32, message = "Shift name must be between 1 and 32 characters")
        String shiftName,

        @NoHtml
        @Size(min = 1, max = 255, message = "Description must be between 1 and 255 characters")
        String description,

        @NotBlank(message = "Color must not be blank")
        @NoHtml
        @Size(min = 6, max = 6, message = "Color must be a valid hex code without #, e.g. 'FF5733'")
        String color,

        @NotNull(message = "Start time must not be null")
        OffsetTime start,

        @NotNull(message = "End time must not be null")
        OffsetTime end,

        @NotNull(message = "Company ID must not be null")
        UUID companyId,

        //TODO: Immagino sia per fare il controllo che tra il nome vecchio e il nuovo
        @NotBlank(message = "Shift name must not be blank")
        @NoHtml
        @Size(min = 3, max = 32, message = "Old shift name must be between 3 and 32 characters")
        String oldShiftName
) {
}
