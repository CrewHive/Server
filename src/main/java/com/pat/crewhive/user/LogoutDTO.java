package com.pat.crewhive.user;


import com.pat.crewhive.security.sanitizer.annotation.NoHtml;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class LogoutDTO {

    @NotNull
    private UUID userId;

    @NotBlank(message = "Refresh token cannot be blank")
    @NoHtml
    private String refreshToken;
}
