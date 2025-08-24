package com.pat.crewhive.dto.manager;

import com.pat.crewhive.security.sanitizer.annotation.NoHtml;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class UpdateUserRoleDTO {

    @NotBlank(message = "New role cannot be blank")
    @Min(value = 1, message = "Role ID must be greater than 0")
    @Max(value = 15, message = "Role ID must be less than or equal to 10")
    @NoHtml
    private String newRole;

    @NotEmpty(message = "User ID cannot be empty")
    private Long userId;
}
