package com.pat.hours_calculator.model.time_management.entities.shifttype;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.*;

import java.time.OffsetTime;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Entity
@Table(name = "shift_type")
public class ShiftType {

    @Id
    @Column(name = "shift_type_id", nullable = false, unique = true)
    @Setter(AccessLevel.NONE)
    private Long id;

    @Column(name = "shiftName", nullable = false)
    private String shiftName;

    @Column(name = "start", nullable = false)
    private OffsetTime start;

    @Column(name = "end", nullable = false)
    private OffsetTime end;

    //todo add user?

}
