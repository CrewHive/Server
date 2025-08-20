package com.pat.crewhive.dto.Company;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
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
    private String companyName;

    //todo vedi se è meglio userid o username
    @NotNull(message = "User ID is required")
    private Long userId;
}
