package com.pat.hours_calculator.dto;

public class UserDTO {

    private Long userId;
    private String email;
    private String username;
    private String password;
    private String companyName;

    public UserDTO() {
    }

    public UserDTO(Long userId, String email, String username, String companyName) {
        this.userId = userId;
        this.email = email;
        this.username = username;
        this.companyName = companyName;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getCompanyName() {
        return companyName;
    }

    public void setCompanyName(String companyName) {
        this.companyName = companyName;
    }
}
