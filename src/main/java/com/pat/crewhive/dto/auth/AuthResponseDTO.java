package com.pat.crewhive.dto.auth;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class AuthResponseDTO {

    @NotBlank(message = "accessToken cannot be blank")
    private String accessToken;

    @NotBlank(message = "refreshToken cannot be blank")
    private String refreshToken;

}
