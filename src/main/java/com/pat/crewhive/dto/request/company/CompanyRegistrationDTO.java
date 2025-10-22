package com.pat.crewhive.dto.request.company;


import com.pat.crewhive.dto.json.AddressJSON;
import com.pat.crewhive.model.util.CompanyType;
import com.pat.crewhive.security.sanitizer.annotation.NoHtml;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class CompanyRegistrationDTO {

    @NotBlank(message = "Company name cannot be blank")
    @NoHtml
    @Size(min = 2, max = 32)
    private String companyName;

    @NotNull(message = "Company type cannot be blank")
    private CompanyType companyType;

    @Valid
    private AddressJSON address;
}
