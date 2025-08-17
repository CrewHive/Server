package com.pat.crewhive.dto;

import com.pat.crewhive.model.user.contract.Contract;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class UserDTO {

    private String email;
    private String username;
    private String role;
    private Long companyId;
}