package com.pat.crewhive.shiftworked;

import com.pat.crewhive.common.audit.SoftDeletableEntity;
import com.pat.crewhive.user.User;
import jakarta.persistence.*;
import jakarta.validation.constraints.AssertTrue;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;


@Entity
@Table(name = "shift_worked", indexes = {
        @Index(name = "idx_shift_worked_user_id", columnList = "user_id"),
        @Index(name = "idx_shift_worked_start_shift", columnList = "start_shift"),
        @Index(name = "idx_shift_worked_end_shift", columnList = "end_shift"),
        @Index(name = "idx_shift_worked_date", columnList = "shift_date"),
        @Index(name = "idx_shift_worked_active", columnList = "active"),
        @Index(name = "idx_shift_worked_deleted_at", columnList = "deleted_at"),
        @Index(name = "idx_shift_worked_deleted_by", columnList = "deleted_by")
})
@SQLRestriction("active = true")
@SQLDelete(sql = "UPDATE shift_worked SET active = false, deleted_at = now() WHERE shift_worked_id = ?")
public class ShiftWorked extends SoftDeletableEntity {

    private static final int HOURS_SCALE = 2;
    private static final RoundingMode HOURS_ROUNDING = RoundingMode.HALF_UP;

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "shift_worked_id", nullable = false)
    private UUID shiftWorkedId;

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

    public ShiftWorked() {
    }

    public UUID getShiftWorkedId() {
        return shiftWorkedId;
    }

    public String getShiftName() {
        return shiftName;
    }

    public void setShiftName(String shiftName) {
        this.shiftName = shiftName;
    }

    public OffsetDateTime getStart() {
        return start;
    }

    public void setStart(OffsetDateTime start) {
        this.start = start;
    }

    public OffsetDateTime getEnd() {
        return end;
    }

    public void setEnd(OffsetDateTime end) {
        this.end = end;
    }

    public LocalDate getDate() {
        return date;
    }

    public void setDate(LocalDate date) {
        this.date = date;
    }

    public int getBreakTime() {
        return breakTime;
    }

    public void setBreakTime(int breakTime) {
        this.breakTime = breakTime;
    }

    public BigDecimal getWorkedHours() {
        return workedHours;
    }

    public void setWorkedHours(BigDecimal workedHours) {
        this.workedHours = workedHours;
    }

    public BigDecimal getExtraHours() {
        return extraHours;
    }

    public void setExtraHours(BigDecimal extraHours) {
        this.extraHours = extraHours;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

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
