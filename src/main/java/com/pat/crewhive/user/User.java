package com.pat.crewhive.user;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.pat.crewhive.common.audit.SoftDeletableEntity;
import com.pat.crewhive.company.Company;
import com.pat.crewhive.event.EventUsers;
import com.pat.crewhive.manager.Role;
import com.pat.crewhive.shiftprogrammed.ShiftUser;
import com.pat.crewhive.shiftworked.ShiftWorked;
import com.pat.crewhive.manager.UserRole;
import jakarta.persistence.*;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

import java.math.BigDecimal;
import java.util.*;

@Entity
@Table(name = "users", indexes = {
        @Index(name = "idx_user_username", columnList = "username"),
        @Index(name = "idx_user_company_id", columnList = "company_id"),
        @Index(name = "idx_user_active", columnList = "active"),
        @Index(name = "idx_user_deleted_at", columnList = "deleted_at"),
        @Index(name = "idx_user_deleted_by", columnList = "deleted_by")
})
@SQLRestriction("active = true")
@SQLDelete(sql = "UPDATE users SET active = false, deleted_at = now() WHERE user_id = ?")
public class User extends SoftDeletableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

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

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private Set<EventUsers> personalEvents = new HashSet<>();

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private Set<UserRole> roles = new HashSet<>();

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

    @OneToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL, optional = false, orphanRemoval = true)
    @JoinColumn(name = "user_user_id", nullable = false)
    private UserPreferences userPreferences;

    public UserPreferences getUserPreferences() {
        return userPreferences;
    }

    public void setUserPreferences(UserPreferences userPreferences) {
        this.userPreferences = userPreferences;

        if (userPreferences != null && userPreferences.getUser() != this) {
            userPreferences.setUser(this);
        }
    }

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

    public Set<EventUsers> getPersonalEvents() {
        return personalEvents;
    }

    public void setPersonalEvents(Set<EventUsers> personalEvents) {
        this.personalEvents = personalEvents;
    }

    public Set<UserRole> getRoles() {
        return roles;
    }

    public void setRoles(Set<UserRole> roles) {
        this.roles = roles;
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

    public void addRole (Role role) {

        boolean hasRole = this.roles.stream().anyMatch(ur -> ur.getRole().equals(role));
        if (hasRole) return;

        UserRole userRole = new UserRole(this, role);
        this.roles.add(userRole);
        role.getUsers().add(userRole);
    }

    public void removeRole (Role role) {
        Iterator<UserRole> iterator = this.roles.iterator();
        while (iterator.hasNext()) {
            UserRole userRole = iterator.next();
            if (userRole.getRole().equals(role)) {
                iterator.remove();
                role.getUsers().remove(userRole);
                break;
            }
        }
    }
}
