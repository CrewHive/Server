package com.pat.crewhive.user;


import com.pat.crewhive.security.sanitizer.annotation.NoHtml;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record LogoutDTO(

        @NotNull
        UUID userId,

        @NotBlank(message = "Refresh token cannot be blank")
        @NoHtml
        String refreshToken
) {
}
