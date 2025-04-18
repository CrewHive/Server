package com.pat.hours_calculator.dto;

import com.pat.hours_calculator.dto.json.ContractJSON;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class UserDTO {

    private Long userId;
    private String email;
    private String username;
    private String role;
    private ContractJSON contract;
    private String companyName;

}