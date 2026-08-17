package com.pat.crewhive.user;

import com.pat.crewhive.security.sanitizer.annotation.NoHtml;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record UserDTO(

        @NotBlank(message = "Email cannot be blank")
        @NoHtml
        @Size(min = 1, max = 6)
        String email,

        @NotBlank(message = "First name cannot be blank")
        @NoHtml
        @Size(min = 3, max = 32)
        String firstName,

        @NotBlank(message = "Last name cannot be blank")
        @NoHtml
        @Size(min = 3, max = 32)
        String lastName,

        @NotBlank(message = "Role cannot be blank")
        @NoHtml
        @Size(min = 3, max = 32)
        String role,

        UUID companyId
) {
}
