package com.pat.crewhive.authuser;

import com.pat.crewhive.security.sanitizer.annotation.NoHtml;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegistrationDTO(

        @NotBlank(message="Email cannot be blank")
        @Email
        @Size(min=5, max=64, message="Email must be between 5 and 64 characters")
        @NoHtml
        String email,

        @NotBlank(message="First name cannot be blank")
        @NoHtml
        @Size(min=3, max=32, message="First name must be between 3 and 32 characters")
        String firstName,

        @NotBlank(message="Last name cannot be blank")
        @NoHtml
        @Size(min=3, max=32, message="Last name must be between 3 and 32 characters")
        String lastName,

        @NotBlank(message="Password cannot be blank")
        @Size(min=12, max=20, message="Password must be between 12 and 20 characters")
        @NoHtml
        String password
) {
}
