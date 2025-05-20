package com.pat.hours_calculator.model.time_management.entities.shift;

import com.pat.hours_calculator.model.user.entity.User;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.OffsetTime;

@NoArgsConstructor
@Getter
@Setter
@Entity
@Table(name = "shift", indexes = {
        @Index(name = "idx_shift_user_id", columnList = "user_id"),
        @Index(name = "idx_shift_start_shift", columnList = "start_shift"),
        @Index(name = "idx_shift_end_shift", columnList = "end_shift"),
        @Index(name = "idx_shift_date", columnList = "date"),
})
public class Shift {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "shift_id", nullable = false)
    @Setter(AccessLevel.NONE)
    private Long shift_id;

    @Column(name = "start_shift", nullable = false)
    private OffsetDateTime startShift;

    @Column(name = "end_shift", nullable = false)
    private OffsetDateTime endShift;

    @Column(name = "date", nullable = false)
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

    public Shift(OffsetDateTime startShift,
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