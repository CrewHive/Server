package com.pat.crewhive.dto.request.shift.template;

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
public class PatchShiftTemplateDTO extends CreateShiftTemplateDTO {


    //TODO: Immagino sia per fare il controllo che tra il nome vecchio e il nuovo
    @NotBlank(message = "Shift name must not be blank")
    @NoHtml
    @Size(min = 3, max = 32, message = "Old shift name must be between 3 and 32 characters")
    private String oldShiftName;
}
