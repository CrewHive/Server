package com.pat.hours_calculator.model.time_management.entities.shift.shiftprogrammed;

import com.pat.hours_calculator.model.company.entity.Company;
import com.pat.hours_calculator.model.user.entity.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.OffsetDateTime;

@NoArgsConstructor
@Getter
@Setter
@Entity
@Table(name = "shift_programmed", indexes = {
        @Index(name = "idx_shiftprogrammed_company_id", columnList = "company_id"),
        @Index(name = "idx_shiftprogrammed_user_id", columnList = "user_id"),
        @Index(name = "idx_shiftprogrammed_date", columnList = "date"),
        @Index(name = "idx_shiftprogrammed", columnList = "start_shift")
})
public class ShiftProgrammed {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "shift_programmed_id", nullable = false)
    @Setter(AccessLevel.NONE)
    private Long id;

    @Column(name = "shift_programmed_name", nullable = false)
    private String shiftName;

    @Column(name = "start_shift", nullable = false)
    private OffsetDateTime start;

    @Column(name = "end_shift", nullable = false)
    private OffsetDateTime end;

    @Column(name = "shift_date", nullable = false)
    private LocalDate date;

    @ManyToOne(optional = false)
    @JoinColumn(name = "company_id", nullable = false)
    private Company company;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;

    public ShiftProgrammed(OffsetDateTime start,
                           OffsetDateTime end,
                           Company company,
                           User user) {
        this.start = start;
        this.end = end;
        this.date = start.toLocalDate();
        this.company = company;
        this.user = user;
    }

}
