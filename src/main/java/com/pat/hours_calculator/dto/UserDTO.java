package com.pat.hours_calculator.dto;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class UserDTO {

    private Long userId;
    private String email;
    private String username;
    private String companyName;

    public UserDTO() {
    }

    public UserDTO(Long userId, String email, String username, String companyName) {
        this.userId = userId;
        this.email = email;
        this.username = username;
        this.companyName = companyName;
    }

}
