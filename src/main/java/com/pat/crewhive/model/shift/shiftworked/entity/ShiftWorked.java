package com.pat.crewhive.model.shift.shiftworked.entity;

import com.pat.crewhive.model.user.entity.User;
import jakarta.persistence.*;
import jakarta.validation.constraints.AssertTrue;
import lombok.*;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
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

    private static final int HOURS_SCALE = 2;
    private static final RoundingMode HOURS_ROUNDING = RoundingMode.HALF_UP;

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

    @Column(name = "worked_hours", nullable = false, precision = 6, scale = 2)
    private BigDecimal workedHours;

    @Column(name = "extra_hours", nullable = false, precision = 6, scale = 2)
    private BigDecimal extraHours;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    public ShiftWorked(String shiftName,
                       OffsetDateTime start,
                       OffsetDateTime end,
                       int breakTimeMinutes,
                       BigDecimal extraHours,
                       User user) {

        this.shiftName = shiftName;
        this.start = start;
        this.end = end;
        this.breakTime = breakTimeMinutes;
        this.user = user;
        this.extraHours = extraHours != null ? extraHours : BigDecimal.ZERO;
        this.workedHours = computeWorkedHours(start, end, breakTimeMinutes);
    }

    private static BigDecimal minutesToHours(long minutes) {
        return BigDecimal.valueOf(minutes)
                .divide(BigDecimal.valueOf(60), HOURS_SCALE, HOURS_ROUNDING);
    }

    private static BigDecimal computeWorkedHours(OffsetDateTime start, OffsetDateTime end, int breakMinutes) {

        if (start == null || end == null) return BigDecimal.ZERO;

        if (!end.isAfter(start)) {
            throw new IllegalArgumentException("L'ora di fine turno deve essere successiva all'ora di inizio turno");
        }

        long totalMinutes = Duration.between(start, end).toMinutes();
        BigDecimal hoursTotal = minutesToHours(totalMinutes);
        BigDecimal breakHours = minutesToHours(breakMinutes);
        BigDecimal net = hoursTotal.subtract(breakHours);

        return net.compareTo(BigDecimal.ZERO) > 0 ? net : BigDecimal.ZERO;
    }

    @PrePersist
    @PreUpdate
    private void syncAndRecompute() {

        if (this.start != null) {
            this.date = this.start.toLocalDate();
        }

        this.workedHours = computeWorkedHours(this.start, this.end, this.breakTime);

        if (this.extraHours == null) {
            this.extraHours = BigDecimal.ZERO;
        }
    }

    // Validazione bean: utile con @Valid sul DTO/Controller
    @AssertTrue(message = "La fine turno deve essere successiva all'inizio turno")
    private boolean isChronologicallyValid() {
        return start != null && end != null && end.isAfter(start);
    }
}
