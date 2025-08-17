package com.pat.crewhive.dto;


import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class RotateRequestDTO {

    @NotBlank(message = "Refresh token must not be blank")
    private String refreshToken;
}
