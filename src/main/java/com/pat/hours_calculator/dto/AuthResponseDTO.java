package com.pat.hours_calculator.dto;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class AuthResponseDTO {

    private String accessToken;

    private String refreshToken;

    public AuthResponseDTO() {}

    public AuthResponseDTO(String accessToken, String refreshToken) {
        this.accessToken = accessToken;
        this.refreshToken = refreshToken;
    }

}
