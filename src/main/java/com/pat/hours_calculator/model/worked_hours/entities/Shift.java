package com.pat.hours_calculator.model.worked_hours.entities;

import com.pat.hours_calculator.model.user.entities.User;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.OffsetTime;

@Getter
@Setter
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

}