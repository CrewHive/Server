package com.pat.crewhive.model.shift.shiftworked.entity;

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
    private Long shiftWorkedId;

    @Column(name = "shift_name", nullable = false)
    private String shiftName;

    @Column(name = "start_shift", nullable = false)
    private OffsetDateTime start;

    @Column(name = "end_shift", nullable = false)
    private OffsetDateTime end;

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

    public ShiftWorked(OffsetDateTime start,
                       OffsetDateTime end,
                       int breakTime,
                       float extraHours,
                       User user) {
        this.start = start;
        this.end = end;
        this.date = start.toLocalDate();
        this.breakTime = breakTime;
        this.workedHours =  (end.getHour() + (float)end.getMinute() / 60 + (float)end.getSecond() / 3600) - (start.getHour() + (float)start.getMinute() / 60 + (float)start.getSecond() / 3600);
        this.extraHours = extraHours;
        this.user = user;
    }

}