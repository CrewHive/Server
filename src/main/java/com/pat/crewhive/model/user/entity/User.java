package com.pat.crewhive.model.user.entity;


import com.pat.crewhive.model.company.entity.Company;
import com.pat.crewhive.model.time_management.entity.event.EventUsers;
import com.pat.crewhive.model.time_management.entity.shift.shiftworked.ShiftWorked;
import com.pat.crewhive.model.time_management.entity.shift.shiftprogrammed.ShiftProgrammed;
import com.pat.crewhive.model.user.contract.Contract;
import com.pat.crewhive.model.user.entity.role.UserRole;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

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
    private List<ShiftWorked> shiftWorked = new ArrayList<>();

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id")
    private Company company;

    @Column(name = "is_working", nullable = false)
    private boolean isWorking;

    @OneToMany(mappedBy = "user", cascade = {CascadeType.DETACH, CascadeType.PERSIST})
    private Set<EventUsers> personalEvents = new LinkedHashSet<>();

    @OneToMany(mappedBy = "user", cascade = CascadeType.REMOVE, orphanRemoval = true, fetch = FetchType.LAZY)
    @OrderBy("start ASC")
    private Set<ShiftProgrammed> shiftProgrammed = new LinkedHashSet<>();

    @OneToOne(mappedBy = "user", cascade = {CascadeType.REMOVE, CascadeType.PERSIST}, optional = false, orphanRemoval = true)
    private UserRole role;

    @OneToOne(mappedBy = "user", cascade = CascadeType.REMOVE, orphanRemoval = true)
    private Contract contract;

    public User(String username, String email, String password, Company company) {
        this.email = email;
        this.username = username;
        this.password = password;
        this.company = company;
        this.isWorking = false;
    }
}
