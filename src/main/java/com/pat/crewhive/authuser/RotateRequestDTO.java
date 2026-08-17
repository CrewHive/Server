package com.pat.crewhive.authuser;

import com.pat.crewhive.security.sanitizer.annotation.NoHtml;
import jakarta.validation.constraints.NotBlank;

public record RotateRequestDTO(

        @NotBlank(message = "Refresh token must not be blank")
        @NoHtml
        String refreshToken
) {
}
