package com.pat.hours_calculator.model.user.entity;


import com.pat.hours_calculator.model.company.entity.Company;
import com.pat.hours_calculator.model.json.ContractJSON;
import com.pat.hours_calculator.model.worked_hours.entities.Shift;
import io.hypersistence.utils.hibernate.type.json.JsonType;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.Type;

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

    @NotNull(message = "Email is required")
    @Column(name="email", unique = true)
    private String email;

    @NotNull(message = "Username is required")
    @Column(name="username", nullable = false, unique = true)
    private String username;

    @NotNull(message = "Password is required")
    @Column(name="password", nullable = false)
    private String password;

    @OneToMany(mappedBy = "user", cascade = CascadeType.REMOVE, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<Shift> shifts = new ArrayList<>();

    @NotNull(message = "Company is required")
    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id")
    private Company company;

    @NotNull(message = "Role is required")
    @Column(name = "role", nullable = false)
    private String role;

    @Type(JsonType.class)
    @Column(name = "contract", nullable = false, columnDefinition = "jsonb")
    private ContractJSON contract;

    @Column(name = "is_working", nullable = false)
    private boolean isWorking;


    public User() {
    }

    public User(String username, String email, String password, String role, Company company, ContractJSON contract) {
        this.email = email;
        this.username = username;
        this.password = password;
        this.role = role;
        this.company = company;
        this.contract = contract;
        this.isWorking = false;
    }
}
