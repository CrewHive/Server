package com.pat.crewhive.dto.json;


import com.pat.crewhive.security.sanitizer.annotation.NoHtml;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AddressJSON {

    @NotBlank(message = "Street cannot be blank")
    @NoHtml
    private String street;

    @NotBlank(message = "City cannot be blank")
    @NoHtml
    private String city;

    @NotBlank(message = "ZIP code cannot be blank")
    @Pattern(regexp = "\\d{5}", message = "ZIP code must be 5 digits")
    @NoHtml
    private String zipCode;

    @NotBlank(message = "Province cannot be blank")
    @Size(min = 2, max = 2, message = "Province must be 2 letters")
    @NoHtml
    private String province;

    @NotBlank(message = "Country cannot be blank")
    @NoHtml
    private String country;
}
