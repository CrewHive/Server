package com.pat.hours_calculator.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import com.pat.hours_calculator.dto.json.ContractJSON;

@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class RegistrationDTO {

    @NotBlank(message="Username cannot be blank")
    private String username;

    @NotBlank(message="Email cannot be blank")
    private String email;

    @NotBlank(message="Password cannot be blank")
    private String password;

    @NotBlank(message="Role cannot be blank")
    private String role;

    @NotBlank(message="Company name cannot be blank")
    private String companyName;

    @NotBlank(message="Contract must exists")
    private ContractJSON contract;
}
