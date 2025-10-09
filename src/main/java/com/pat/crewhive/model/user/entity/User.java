package com.pat.crewhive.model.user.entity;

import com.fasterxml.jackson.annotation.*;
import com.pat.crewhive.model.company.entity.Company;
import com.pat.crewhive.model.event.EventUsers;
import com.pat.crewhive.model.shift.shiftprogrammed.ShiftUser;
import com.pat.crewhive.model.shift.shiftworked.entity.ShiftWorked;
import com.pat.crewhive.model.role.UserRole;
import com.pat.crewhive.model.util.ContractType;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.*;

@NoArgsConstructor
@Entity
@Table(name = "users", indexes = {
        @Index(name = "idx_user_username", columnList = "username"),
        @Index(name = "idx_user_company_id", columnList = "company_id")
})
@Getter
@Setter
@JsonIdentityInfo(
        generator = ObjectIdGenerators.PropertyGenerator.class,
        property = "userId")
public class User {
//todo modifica annotazioni json
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Setter(AccessLevel.NONE)
    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name="email", unique = true)
    private String email;

    @Column(name="username", nullable = false, unique = true)
    private String username;

    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    @Column(name="password", nullable = false)
    private String password;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id")
    @JsonIgnore
    private Company company;

    @Column(name = "is_working", nullable = false)
    private boolean isWorking;

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @JsonManagedReference
    private Set<EventUsers> personalEvents = new HashSet<>();

    @OneToOne(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private UserRole role;

    @Enumerated(EnumType.STRING)
    @Column(name = "contract_type")
    private ContractType contractType;

    @Column(name = "workable_hours_per_week", nullable = false)
    private int workableHoursPerWeek;

    @Column(name = "overtime_hours", nullable = false)
    private BigDecimal overtimeHours;

    @Column(name = "vacation_days_accumulated", nullable = false)
    private BigDecimal vacationDaysAccumulated;

    @Column(name = "vacation_days_taken", nullable = false)
    private BigDecimal vacationDaysTaken;

    @Column(name = "leave_days_accumulated", nullable = false)
    private BigDecimal leaveDaysAccumulated;

    @Column(name = "leave_days_taken", nullable = false)
    private BigDecimal leaveDaysTaken;

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private Set<ShiftWorked> shiftWorked = new HashSet<>();

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @JsonManagedReference
    private Set<ShiftUser> shiftUsers = new HashSet<>();

    public User(String username, String email, String password) {
        this.email = email;
        this.username = username;
        this.password = password;
        this.isWorking = false;
        this.workableHoursPerWeek = 0;
        this.overtimeHours = BigDecimal.ZERO;
        this.vacationDaysAccumulated = BigDecimal.ZERO;
        this.vacationDaysTaken = BigDecimal.ZERO;
        this.leaveDaysAccumulated = BigDecimal.ZERO;
        this.leaveDaysTaken = BigDecimal.ZERO;
    }
}
