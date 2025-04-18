package com.pat.hours_calculator.dto.json;


import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Address {

    private String street;
    private String city;
    private String zipCode;
    private String province;
    private String country;
}
