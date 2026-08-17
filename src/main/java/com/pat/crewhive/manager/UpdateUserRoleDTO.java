package com.pat.crewhive.manager;

import com.pat.crewhive.security.sanitizer.annotation.NoHtml;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

import java.util.UUID;

public record UpdateUserRoleDTO(

        @NotBlank(message = "New role cannot be blank")
        @Min(value = 1, message = "Role ID must be greater than 0")
        @Max(value = 15, message = "Role ID must be less than or equal to 10")
        @NoHtml
        String newRole,

        @NotEmpty(message = "User ID cannot be empty")
        UUID userId
) {
}
