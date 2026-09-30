package com.pat.crewhive.authuser;

import com.pat.crewhive.security.sanitizer.annotation.NoHtml;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record VerifyEmailDTO(

        @NotBlank(message = "Token must not be blank")
        @Size(max = 128, message = "Token must be at most 128 characters")
        @NoHtml
        String token
) {
}
