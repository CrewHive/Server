package com.pat.crewhive.manager;

import com.pat.crewhive.security.sanitizer.annotation.NoHtml;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record UpdateUserRoleDTO(

        @NotBlank(message = "New role cannot be blank")
        @Size(max = 50, message = "Role name must be at most 50 characters")
        @NoHtml
        String newRole,

        @NotNull(message = "User ID is required")
        UUID userId
) {
}
