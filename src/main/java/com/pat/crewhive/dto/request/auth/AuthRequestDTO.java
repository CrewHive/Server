package com.pat.crewhive.dto.request.auth;

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
public class AuthRequestDTO {

    @NotBlank(message = "Email cannot be blank")
    @NoHtml
    @Size(min = 3, max = 32, message = "Email must be of an admissible format")
    private String email;

    @NotBlank(message = "Password cannot be blank")
    @NoHtml
    private String password;

}
