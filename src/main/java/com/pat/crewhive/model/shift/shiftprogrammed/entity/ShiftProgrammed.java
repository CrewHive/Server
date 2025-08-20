package com.pat.crewhive.model.shift.shiftprogrammed.entity;

import com.pat.crewhive.model.user.entity.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.OffsetDateTime;

@NoArgsConstructor
@Getter
@Setter
@Entity
@Table(name = "shift_programmed", indexes = {
        @Index(name = "idx_shiftprogrammed_user_id", columnList = "user_id"),
        @Index(name = "idx_shiftprogrammed_date", columnList = "shift_date"),
        @Index(name = "idx_shiftprogrammed", columnList = "start_shift")
})
public class ShiftProgrammed {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "shift_programmed_id", nullable = false)
    @Setter(AccessLevel.NONE)
    private Long shiftProgrammedId;

    @Column(name = "shift_programmed_name", nullable = false)
    private String shiftName;

    @Column(name = "start_shift", nullable = false)
    private OffsetDateTime start;

    @Column(name = "end_shift", nullable = false)
    private OffsetDateTime end;

    @Column(name = "shift_date", nullable = false)
    private LocalDate date;

    @Column(name = "description")
    private String description;

    @Column(name = "color", nullable = false)
    private String color;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    public ShiftProgrammed(OffsetDateTime start,
                           OffsetDateTime end,
                           User user) {
        this.start = start;
        this.end = end;
        this.date = start.toLocalDate();
        this.user = user;
    }

}
