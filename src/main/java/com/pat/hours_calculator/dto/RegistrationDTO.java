package com.pat.hours_calculator.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;
import com.pat.hours_calculator.model.json.ContractJSON;

@Setter
@Getter
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

    public RegistrationDTO() {
    }

    public RegistrationDTO(String username, String email, String password, String role, String companyName, ContractJSON contract) {
        this.username = username;
        this.email = email;
        this.password = password;
        this.role = role;
        this.companyName = companyName;
        this.contract = contract;
    }
}
