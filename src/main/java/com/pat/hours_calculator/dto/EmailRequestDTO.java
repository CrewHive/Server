package com.pat.hours_calculator.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class EmailRequestDTO {

    //todo! Subject= "email@gmail.com: Subject"
    private String subject;
    private String body;
}
