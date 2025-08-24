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
public class UserDTO {

    @NotBlank(message = "Email cannot be blank")
    @NoHtml
    @Size(min = 1, max = 6)
    private String email;

    @NotBlank(message = "Username cannot be blank")
    @NoHtml
    @Size(min = 3, max = 32)
    private String username;

    @NotBlank(message = "Role cannot be blank")
    @NoHtml
    @Size(min = 3, max = 32)
    private String role;

    private Long companyId;
}