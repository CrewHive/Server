package com.pat.crewhive.company;


import com.pat.crewhive.security.sanitizer.annotation.NoHtml;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CompanyRegistrationDTO(

        @NotBlank(message = "Company name cannot be blank")
        @NoHtml
        @Size(min = 2, max = 32)
        String companyName,

        @NotNull(message = "Company type cannot be blank")
        CompanyType companyType,

        @Valid
        AddressJSON address
) {
}
