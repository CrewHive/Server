package com.pat.crewhive.dto.request.shift.template;


import com.pat.crewhive.security.sanitizer.annotation.NoHtml;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.OffsetTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CreateShiftTemplateDTO {

    @NotBlank(message = "Shift name must not be blank")
    @NoHtml
    @Size(min = 1, max = 32, message = "Shift name must be between 1 and 32 characters")
    private String shiftName;

    @NoHtml
    @Size(min = 1, max = 255, message = "Description must be between 1 and 255 characters")
    private String description;

    @NotBlank(message = "Color must not be blank")
    @NoHtml
    @Size(min = 6, max = 6, message = "Color must be a valid hex code without #, e.g. 'FF5733'")
    private String color;

    @NotNull(message = "Start time must not be null")
    private OffsetTime start;

    @NotNull(message = "End time must not be null")
    private OffsetTime end;

    @NotNull(message = "Company ID must not be null")
    private Long companyId;
}
