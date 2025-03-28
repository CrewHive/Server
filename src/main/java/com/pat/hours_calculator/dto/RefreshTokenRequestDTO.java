package com.pat.hours_calculator.dto;


import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class RefreshTokenRequestDTO {

    private String refreshToken;

    public RefreshTokenRequestDTO() {
    }

    public RefreshTokenRequestDTO(String refreshToken) {
        this.refreshToken = refreshToken;
    }

}
