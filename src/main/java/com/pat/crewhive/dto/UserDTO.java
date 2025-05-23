package com.pat.crewhive.dto;

import com.pat.crewhive.dto.json.ContractJSON;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class UserDTO {

    //todo: check if it needs the @NotNull annotation because for now you don't receive it in the request
    private Long userId;
    private String email;
    private String username;
    private String role;
    private ContractJSON contract;
    private String companyName;

}