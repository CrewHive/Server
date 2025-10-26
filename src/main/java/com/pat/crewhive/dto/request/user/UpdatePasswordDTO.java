package com.pat.crewhive.dto.request.user;

import com.pat.crewhive.security.sanitizer.annotation.NoHtml;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UpdatePasswordDTO {

    @NotBlank(message = "Old password cannot be blank")
    @NoHtml
    private String oldPassword;

    @NotBlank(message = "New password cannot be blank")
    @NoHtml
    private String newPassword;
}
