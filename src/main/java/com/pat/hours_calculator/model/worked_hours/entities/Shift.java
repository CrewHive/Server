package com.pat.hours_calculator.model.worked_hours.entities;

import com.pat.hours_calculator.model.user.entities.User;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.time.OffsetTime;

@Entity
@Table(name = "shift")
public class Shift {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "shift_id", nullable = false)
    private Long shift_id;

    @NotNull(message = "Start shift time is required")
    @Column(name = "start_shift", nullable = false)
    private OffsetTime startShift;


    @NotNull(message = "End shift time is required")
    @Column(name = "end_shift", nullable = false)
    private OffsetTime endShift;

    @NotNull(message = "Shift date is required")
    @Column(name = "shift_date", nullable = false)
    private LocalDate shiftDate;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    public Shift() {
    }

    public Shift(OffsetTime startShift, OffsetTime endShift, LocalDate shiftDate, User user) {
        this.startShift = startShift;
        this.endShift = endShift;
        this.shiftDate = shiftDate;
        this.user = user;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public LocalDate getShiftDate() {
        return shiftDate;
    }

    public OffsetTime getEndShift() {
        return endShift;
    }

    public OffsetTime getStartShift() {
        return startShift;
    }

    public Long getShiftId() {
        return shift_id;
    }

    public void setShiftId(Long id) {
        this.shift_id = id;
    }

}