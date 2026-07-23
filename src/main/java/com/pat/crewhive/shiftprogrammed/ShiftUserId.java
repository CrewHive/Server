package com.pat.crewhive.shiftprogrammed;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.*;

import java.io.Serializable;
import java.util.UUID;

@Embeddable
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class ShiftUserId implements Serializable {

    @Column(name = "shift_programmed_id", nullable = false)
    private UUID shiftProgrammedId;

    @Column(name = "user_id", nullable = false)
    private UUID userId;
}
