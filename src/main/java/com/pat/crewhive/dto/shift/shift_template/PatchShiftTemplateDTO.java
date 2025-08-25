package com.pat.crewhive.dto.shift.shift_template;

import com.pat.crewhive.security.sanitizer.annotation.NoHtml;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PatchShiftTemplateDTO extends CreateShiftTemplateDTO{

    @NotBlank(message = "Shift name must not be blank")
    @NoHtml
    @Size(min = 3, max = 32, message = "Shift name must be between 3 and 32 characters")
    private String oldShiftName;
}
