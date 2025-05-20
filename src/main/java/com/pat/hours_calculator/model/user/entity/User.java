package com.pat.hours_calculator.model.user.entity;


import com.pat.hours_calculator.model.company.entity.Company;
import com.pat.hours_calculator.dto.json.ContractJSON;
import com.pat.hours_calculator.model.time_management.entities.event.PersonalEvent;
import com.pat.hours_calculator.model.time_management.entities.event.PersonalEventUsers;
import com.pat.hours_calculator.model.time_management.entities.shift.Shift;
import io.hypersistence.utils.hibernate.type.json.JsonType;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.Type;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@NoArgsConstructor
@Entity
@Table(name = "users", indexes = {
        @Index(name = "idx_user_username", columnList = "username"),
        @Index(name = "idx_user_company_id", columnList = "company_id")
})
@Getter
@Setter
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Setter(AccessLevel.NONE)
    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name="email", unique = true)
    @Setter(AccessLevel.NONE)
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

    @Type(JsonType.class)
    @Column(name = "contract", nullable = false, columnDefinition = "jsonb")
    private ContractJSON contract;

    @Column(name = "is_working", nullable = false)
    private boolean isWorking;

    @OneToMany(mappedBy = "user", cascade = CascadeType.DETACH)
    private Set<PersonalEventUsers> personalEventLinks = new LinkedHashSet<>();

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
