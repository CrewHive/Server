package com.pat.crewhive.dto.user;

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

    @NotBlank
    private String oldPassword;

    @NotBlank
    private String newPassword;
}
