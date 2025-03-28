package com.pat.hours_calculator.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class AuthRequestDTO {

    @NotBlank(message = "Username cannot be blank")
    private String username;

    @NotBlank(message = "Password cannot be blank")
    private String password;

    public AuthRequestDTO() {
    }

    public AuthRequestDTO(String username, String password) {
        this.username = username;
        this.password = password;
    }

}
