package com.pat.crewhive.dto.User;


import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class LogoutDTO {

    @NotBlank(message = "Username cannot be blank")
    private String username;

    @NotBlank(message = "Refresh token cannot be blank")
    private String refreshToken;
}
