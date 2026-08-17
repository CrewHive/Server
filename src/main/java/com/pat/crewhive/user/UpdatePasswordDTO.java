package com.pat.crewhive.user;

import com.pat.crewhive.security.sanitizer.annotation.NoHtml;
import jakarta.validation.constraints.NotBlank;

public record UpdatePasswordDTO(

        @NotBlank(message = "Old password cannot be blank")
        @NoHtml
        String oldPassword,

        @NotBlank(message = "New password cannot be blank")
        @NoHtml
        String newPassword
) {
}
