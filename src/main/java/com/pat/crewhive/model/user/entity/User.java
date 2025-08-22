package com.pat.crewhive.model.user.entity;


import com.pat.crewhive.model.company.entity.Company;
import com.pat.crewhive.model.event.EventUsers;
import com.pat.crewhive.model.shift.shiftworked.entity.ShiftWorked;
import com.pat.crewhive.model.shift.shiftprogrammed.entity.ShiftProgrammed;
import com.pat.crewhive.model.role.UserRole;
import com.pat.crewhive.model.util.ContractType;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@NoArgsConstructor
@Entity
@Table(name = "users", indexes = {
        @Index(name = "idx_user_id", columnList = "user_id"),
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
    private String email;

    @Column(name="username", nullable = false, unique = true)
    private String username;

    @Column(name="password", nullable = false)
    private String password;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id")
    private Company company;

    @Column(name = "is_working", nullable = false)
    private boolean isWorking;

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private Set<EventUsers> personalEvents = new LinkedHashSet<>();

    @OneToOne(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private UserRole role;

    @Enumerated(EnumType.STRING)
    @Column(name = "contract_type")
    private ContractType contractType;

    @Column(name = "workable_hours_per_week", nullable = false)
    private int workableHoursPerWeek;

    @Column(name = "overtime_hours", nullable = false)
    private int overtimeHours;

    @Column(name = "vacation_days_accumulated", nullable = false)
    private BigDecimal vacationDaysAccumulated;

    @Column(name = "vacation_days_taken", nullable = false)
    private BigDecimal vacationDaysTaken;

    @Column(name = "leave_days_accumulated", nullable = false)
    private BigDecimal leaveDaysAccumulated;

    @Column(name = "leave_days_taken", nullable = false)
    private BigDecimal leaveDaysTaken;

    //todo se salvo dal lato user lascia all se uso lato shift usa remove uguale per i programmed
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<ShiftWorked> shiftWorked = new ArrayList<>();

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @OrderBy("start ASC")
    private Set<ShiftProgrammed> shiftProgrammed = new LinkedHashSet<>();

    public User(String username, String email, String password) {
        this.email = email;
        this.username = username;
        this.password = password;
        this.isWorking = false;
        this.workableHoursPerWeek = 0;
        this.overtimeHours = 0;
        this.vacationDaysAccumulated = BigDecimal.ZERO;
        this.vacationDaysTaken = BigDecimal.ZERO;
        this.leaveDaysAccumulated = BigDecimal.ZERO;
        this.leaveDaysTaken = BigDecimal.ZERO;
    }
}
