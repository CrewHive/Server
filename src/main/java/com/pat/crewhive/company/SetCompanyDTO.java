package com.pat.crewhive.company;

import com.pat.crewhive.security.sanitizer.annotation.NoHtml;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record SetCompanyDTO(

        @NotBlank(message = "Company name is required")
        @NoHtml
        @Size(min = 2, max = 32)
        String companyName,

        @NotNull(message = "User ID is required")
        UUID userId
) {
}
