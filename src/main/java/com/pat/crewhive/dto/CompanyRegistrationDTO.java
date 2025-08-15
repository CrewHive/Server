package com.pat.crewhive.dto;


import com.pat.crewhive.dto.json.AddressJSON;
import com.pat.crewhive.model.util.CompanyType;
import jakarta.validation.constraints.NotBlank;
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

    @NotBlank(message = "Company type cannot be blank")
    private CompanyType companyType;

    @NotBlank(message = "Address cannot be blank")
    private AddressJSON address;
}
