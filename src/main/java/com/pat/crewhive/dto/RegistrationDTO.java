package com.pat.crewhive.dto;

import com.pat.crewhive.model.user.contract.Contract;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class RegistrationDTO {

    @NotBlank(message="Username cannot be blank")
    private String username;

    @NotBlank(message="Email cannot be blank")
    @Email
    @Size(min=5, max=32, message="Email must be between 5 and 32 characters")
    private String email;

    @NotBlank(message="Password cannot be blank")
    @Size(min=8, max=32, message="Password must be between 8 and 32 characters")
    private String password;

}
