package com.pat.crewhive.authuser;

import jakarta.validation.constraints.NotBlank;

public record AuthResponseDTO(

        @NotBlank(message = "accessToken cannot be blank")
        String accessToken,

        @NotBlank(message = "refreshToken cannot be blank")
        String refreshToken
) {
}
