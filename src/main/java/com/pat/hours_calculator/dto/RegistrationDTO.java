package com.pat.hours_calculator.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class RegistrationDTO {

    @NotBlank(message="Username cannot be blank")
    private String username;

    @NotBlank(message="Password cannot be blank")
    private String password;

    @NotBlank(message="Role cannot be blank")
    private String role;

    @NotBlank(message="Company name cannot be blank")
    private String companyName;

    public RegistrationDTO() {
    }

    public RegistrationDTO(String username, String password, String role, String companyName) {
        this.username = username;
        this.password = password;
        this.role = role;
        this.companyName = companyName;
    }

}
