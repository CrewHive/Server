package com.pat.crewhive.user;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.pat.crewhive.company.Company;
import com.pat.crewhive.event.EventUsers;
import com.pat.crewhive.shiftprogrammed.ShiftUser;
import com.pat.crewhive.shiftworked.ShiftWorked;
import com.pat.crewhive.manager.UserRole;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.util.*;

@Entity
@Table(name = "users", indexes = {
        @Index(name = "idx_user_username", columnList = "username"),
        @Index(name = "idx_user_company_id", columnList = "company_id")
})
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name="email", unique = true)
    private String email;

    @Column(name="first_name", nullable = false)
    private String firstName;

    @Column(name="last_name", nullable = false)
    private String lastName;

    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    @Column(name="password", nullable = false)
    private String password;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id")
    private Company company;

    @Column(name = "is_working", nullable = false)
    private boolean isWorking;

    /**
     * Whether the account is active. Set to false by {@code UserService.deleteAccount}
     * (soft-delete) instead of physically removing the row, so that historical records
     * (e.g. {@link com.pat.crewhive.shiftworked.ShiftWorked}) keep their reference to the user.
     */
    @Column(name = "active", nullable = false)
    private boolean active = true;

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private Set<EventUsers> personalEvents = new HashSet<>();

    @OneToOne(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
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

    @OneToMany(mappedBy = "user", fetch = FetchType.LAZY)
    private Set<ShiftWorked> shiftWorked = new HashSet<>();

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private Set<ShiftUser> shiftUsers = new HashSet<>();

    public User() {
    }

    public User(String email, String firstName, String lastName, String password) {
        this.email = email;
        this.firstName = firstName;
        this.lastName = lastName;
        this.password = password;
        this.isWorking = false;
        this.workableHoursPerWeek = 0;
        this.overtimeHours = BigDecimal.ZERO;
        this.vacationDaysAccumulated = BigDecimal.ZERO;
        this.vacationDaysTaken = BigDecimal.ZERO;
        this.leaveDaysAccumulated = BigDecimal.ZERO;
        this.leaveDaysTaken = BigDecimal.ZERO;
    }

    public UUID getUserId() {
        return userId;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public Company getCompany() {
        return company;
    }

    public void setCompany(Company company) {
        this.company = company;
    }

    public boolean isWorking() {
        return isWorking;
    }

    public void setWorking(boolean working) {
        isWorking = working;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public Set<EventUsers> getPersonalEvents() {
        return personalEvents;
    }

    public void setPersonalEvents(Set<EventUsers> personalEvents) {
        this.personalEvents = personalEvents;
    }

    public UserRole getRole() {
        return role;
    }

    public void setRole(UserRole role) {
        this.role = role;
    }

    public ContractType getContractType() {
        return contractType;
    }

    public void setContractType(ContractType contractType) {
        this.contractType = contractType;
    }

    public int getWorkableHoursPerWeek() {
        return workableHoursPerWeek;
    }

    public void setWorkableHoursPerWeek(int workableHoursPerWeek) {
        this.workableHoursPerWeek = workableHoursPerWeek;
    }

    public BigDecimal getOvertimeHours() {
        return overtimeHours;
    }

    public void setOvertimeHours(BigDecimal overtimeHours) {
        this.overtimeHours = overtimeHours;
    }

    public BigDecimal getVacationDaysAccumulated() {
        return vacationDaysAccumulated;
    }

    public void setVacationDaysAccumulated(BigDecimal vacationDaysAccumulated) {
        this.vacationDaysAccumulated = vacationDaysAccumulated;
    }

    public BigDecimal getVacationDaysTaken() {
        return vacationDaysTaken;
    }

    public void setVacationDaysTaken(BigDecimal vacationDaysTaken) {
        this.vacationDaysTaken = vacationDaysTaken;
    }

    public BigDecimal getLeaveDaysAccumulated() {
        return leaveDaysAccumulated;
    }

    public void setLeaveDaysAccumulated(BigDecimal leaveDaysAccumulated) {
        this.leaveDaysAccumulated = leaveDaysAccumulated;
    }

    public BigDecimal getLeaveDaysTaken() {
        return leaveDaysTaken;
    }

    public void setLeaveDaysTaken(BigDecimal leaveDaysTaken) {
        this.leaveDaysTaken = leaveDaysTaken;
    }

    public Set<ShiftWorked> getShiftWorked() {
        return shiftWorked;
    }

    public void setShiftWorked(Set<ShiftWorked> shiftWorked) {
        this.shiftWorked = shiftWorked;
    }

    public Set<ShiftUser> getShiftUsers() {
        return shiftUsers;
    }

    public void setShiftUsers(Set<ShiftUser> shiftUsers) {
        this.shiftUsers = shiftUsers;
    }
}
