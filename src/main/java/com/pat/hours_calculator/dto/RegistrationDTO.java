package com.pat.hours_calculator.dto;

import jakarta.validation.constraints.NotBlank;

public class RegistrationDTO {

    @NotBlank(message="Username cannot be blank")
    private String username;

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    @NotBlank(message="Email cannot be blank")
    private String email;

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    @NotBlank(message="Password cannot be blank")
    private String password;

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    @NotBlank(message="Role cannot be blank")
    private String role;

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    @NotBlank(message="Company name cannot be blank")
    private String companyName;

    public String getCompanyName() {
        return companyName;
    }

    public void setCompanyName(String companyName) {
        this.companyName = companyName;
    }

    public RegistrationDTO() {
    }

    public RegistrationDTO(String username, String email, String password, String role, String companyName) {
        this.username = username;
        this.email = email;
        this.password = password;
        this.role = role;
        this.companyName = companyName;
    }

}
