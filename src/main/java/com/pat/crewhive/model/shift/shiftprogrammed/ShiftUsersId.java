package com.pat.crewhive.model.shift.shiftprogrammed;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.*;

import java.io.Serializable;

@Embeddable
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class ShiftUsersId implements Serializable {

    @Column(name = "shift_programmed_id", nullable = false)
    private Long shiftProgrammedId;

    @Column(name = "user_id", nullable = false)
    private Long userId;
}
