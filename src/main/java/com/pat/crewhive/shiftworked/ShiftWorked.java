package com.pat.crewhive.shiftworked;

import com.pat.crewhive.common.shift.Shift;
import com.pat.crewhive.company.Company;
import com.pat.crewhive.user.User;
import jakarta.persistence.*;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.UUID;


@Entity
@Table(name = "shift_worked", indexes = {
        @Index(name = "idx_shift_worked_user_id", columnList = "user_id"),
        @Index(name = "idx_shift_worked_company_id", columnList = "company_id"),
        @Index(name = "idx_shift_worked_start_shift", columnList = "start_shift"),
        @Index(name = "idx_shift_worked_end_shift", columnList = "end_shift"),
        @Index(name = "idx_shift_worked_date", columnList = "shift_date"),
        @Index(name = "idx_shift_worked_active", columnList = "active"),
        @Index(name = "idx_shift_worked_deleted_at", columnList = "deleted_at"),
        @Index(name = "idx_shift_worked_deleted_by", columnList = "deleted_by")
})
@AttributeOverrides({
        @AttributeOverride(name = "id", column = @Column(name = "shift_worked_id", nullable = false)),
        @AttributeOverride(name = "shiftName", column = @Column(name = "shift_name", nullable = false))
})
@SQLRestriction("active = true")
@SQLDelete(sql = "UPDATE shift_worked SET active = false, deleted_at = now() WHERE shift_worked_id = ? AND version = ?")
public class ShiftWorked extends Shift {

    private static final int HOURS_SCALE = 2;
    private static final RoundingMode HOURS_ROUNDING = RoundingMode.HALF_UP;

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
        return getId();
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
                       User user,
                       Company company) {

        setShiftName(shiftName);
        setStart(start);
        setEnd(end);
        setCompany(company);
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
    private void recomputeWorkedHours() {

        this.workedHours = computeWorkedHours(getStart(), getEnd(), this.breakTime);

        if (this.extraHours == null) {
            this.extraHours = BigDecimal.ZERO;
        }
    }
}
