package com.pat.hours_calculator.model.user.entity;


import com.pat.hours_calculator.model.company.entity.Company;
import com.pat.hours_calculator.model.worked_hours.entities.Shift;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "users")
@Getter
@Setter
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_id", nullable = false)
    private Long user_id;

    @Column(name="email", unique = true)
    private String email;

    @Column(name="username", nullable = false, unique = true)
    private String username;

    @Column(name="password", nullable = false)
    private String password;

    @OneToMany(mappedBy = "user", cascade = CascadeType.REMOVE, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<Shift> shifts = new ArrayList<>();

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id")
    private Company company;

    @Column(name = "role", nullable = false)
    private String role;

    public User() {
    }

    public User(String username, String email, String password, String role) {
        this.username = username;
        this.email = email;
        this.password = password;
        this.role = role;
    }

    public User(String username, String password, String role, Company company) {
        this.username = username;
        this.password = password;
        this.role = role;
        this.company = company;
    }

    public User(String email, String username, String password, String role, Company company) {
        this.email = email;
        this.username = username;
        this.password = password;
        this.role = role;
        this.company = company;
    }
}
