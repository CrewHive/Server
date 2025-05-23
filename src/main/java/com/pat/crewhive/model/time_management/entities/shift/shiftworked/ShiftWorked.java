package com.pat.crewhive.model.time_management.entities.shift.shiftworked;

import com.pat.crewhive.model.user.entity.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.OffsetDateTime;

@NoArgsConstructor
@Getter
@Setter
@Entity
@Table(name = "shift_worked", indexes = {
        @Index(name = "idx_shift_worked_user_id", columnList = "user_id"),
        @Index(name = "idx_shift_worked_start_shift", columnList = "start_shift"),
        @Index(name = "idx_shift_worked_end_shift", columnList = "end_shift"),
        @Index(name = "idx_shift_worked_date", columnList = "shift_date"),
})
public class ShiftWorked {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "shift_worked_id", nullable = false)
    @Setter(AccessLevel.NONE)
    private Long shift_id;

    @Column(name = "start_shift", nullable = false)
    private OffsetDateTime startShift;

    @Column(name = "end_shift", nullable = false)
    private OffsetDateTime endShift;

    @Column(name = "shift_date", nullable = false)
    private LocalDate date;

    @Column(name = "break_time", nullable = false)
    private int breakTime;

    @Column(name = "worked_hours", nullable = false)
    private float workedHours;

    @Column(name = "extra_hours", nullable = false)
    private float extraHours;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    public ShiftWorked(OffsetDateTime startShift,
                       OffsetDateTime endShift,
                       int breakTime,
                       float extraHours,
                       User user) {
        this.startShift = startShift;
        this.endShift = endShift;
        this.date = startShift.toLocalDate();
        this.breakTime = breakTime;
        this.workedHours =  (endShift.getHour() + (float)endShift.getMinute() / 60 + (float)endShift.getSecond() / 3600) - (startShift.getHour() + (float)startShift.getMinute() / 60 + (float)startShift.getSecond() / 3600);
        this.extraHours = extraHours;
        this.user = user;
    }

}