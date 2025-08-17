package com.pat.crewhive.dto.json;


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
    private String street;

    @NotBlank(message = "City cannot be blank")
    private String city;

    @NotBlank(message = "ZIP code cannot be blank")
    @Pattern(regexp = "\\d{5}", message = "ZIP code must be 5 digits")
    private String zipCode;

    @NotBlank(message = "Province cannot be blank")
    @Size(min = 2, max = 2, message = "Province must be 2 letters")
    private String province;

    @NotBlank(message = "Country cannot be blank")
    private String country;
}
