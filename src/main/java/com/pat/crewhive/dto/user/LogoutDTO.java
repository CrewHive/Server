package com.pat.crewhive.dto.user;


import com.pat.crewhive.security.sanitizer.annotation.NoHtml;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
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
    @NoHtml
    private String username;

    @NotBlank(message = "Refresh token cannot be blank")
    @NoHtml
    private String refreshToken;
}
