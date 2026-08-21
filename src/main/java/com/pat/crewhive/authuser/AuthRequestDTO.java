package com.pat.crewhive.authuser;

import com.pat.crewhive.security.sanitizer.annotation.NoHtml;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AuthRequestDTO(

        @NotBlank(message = "Email cannot be blank")
        @NoHtml
        @Size(min = 7, max = 64, message = "Email must be of an admissible format")
        String email,

        @NotBlank(message = "Password cannot be blank")
        @NoHtml
        String password
) {
}
