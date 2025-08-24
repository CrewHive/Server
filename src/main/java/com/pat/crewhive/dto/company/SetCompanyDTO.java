package com.pat.crewhive.dto.company;

import com.pat.crewhive.security.sanitizer.annotation.NoHtml;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SetCompanyDTO {

    @NotBlank(message = "Company name is required")
    @NoHtml
    @Size(min = 2, max = 32)
    private String companyName;

    @NotNull(message = "User ID is required")
    private Long userId;
}
