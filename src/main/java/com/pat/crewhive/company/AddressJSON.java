package com.pat.crewhive.company;


import com.pat.crewhive.security.sanitizer.annotation.NoHtml;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record AddressJSON(

        @NotBlank(message = "Street cannot be blank")
        @NoHtml
        String street,

        @NotBlank(message = "City cannot be blank")
        @NoHtml
        String city,

        @NotBlank(message = "ZIP code cannot be blank")
        @Pattern(regexp = "\\d{5}", message = "ZIP code must be 5 digits")
        @NoHtml
        String zipCode,

        @NotBlank(message = "Province cannot be blank")
        @Size(min = 2, max = 2, message = "Province must be 2 letters")
        @NoHtml
        String province,

        @NotBlank(message = "Country cannot be blank")
        @NoHtml
        String country
) {
}
