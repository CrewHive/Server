package com.pat.hours_calculator.dto;

public class RegistrationDTO {

    private String username;

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    private String email;

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }
    private String password;

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    private String role;

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

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
