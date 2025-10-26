package com.pat.crewhive.dto.request.auth;

import com.pat.crewhive.security.sanitizer.annotation.NoHtml;
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

    @NotBlank(message="Email cannot be blank")
    @Email
    @Size(min=5, max=32, message="Email must be between 5 and 32 characters")
    @NoHtml
    private String email;

    @NotBlank(message="First name cannot be blank")
    @NoHtml
    @Size(min=3, max=32, message="First name must be between 3 and 32 characters")
    private String firstName;

    @NotBlank(message="Last name cannot be blank")
    @NoHtml
    @Size(min=3, max=32, message="Last name must be between 3 and 32 characters")
    private String lastName;

    @NotBlank(message="Password cannot be blank")
    @Size(min=8, max=32, message="Password must be between 8 and 32 characters")
    @NoHtml
    private String password;

}
