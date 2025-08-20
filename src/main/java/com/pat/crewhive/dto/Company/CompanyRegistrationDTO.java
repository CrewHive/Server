package com.pat.crewhive.dto.Company;


import com.pat.crewhive.dto.json.AddressJSON;
import com.pat.crewhive.model.util.CompanyType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
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
    private String companyName;

    @NotNull(message = "Company type cannot be blank")
    private CompanyType companyType;

    @NotNull(message = "Address cannot be blank")
    @Valid
    private AddressJSON address;
}
