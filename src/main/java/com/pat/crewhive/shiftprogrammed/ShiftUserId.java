package com.pat.crewhive.shiftprogrammed;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

@Embeddable
public class ShiftUserId implements Serializable {

    @Column(name = "shift_programmed_id", nullable = false)
    private UUID shiftProgrammedId;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    public ShiftUserId() {
    }

    public ShiftUserId(UUID shiftProgrammedId, UUID userId) {
        this.shiftProgrammedId = shiftProgrammedId;
        this.userId = userId;
    }

    public UUID getShiftProgrammedId() {
        return shiftProgrammedId;
    }

    public void setShiftProgrammedId(UUID shiftProgrammedId) {
        this.shiftProgrammedId = shiftProgrammedId;
    }

    public UUID getUserId() {
        return userId;
    }

    public void setUserId(UUID userId) {
        this.userId = userId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ShiftUserId that)) return false;
        return Objects.equals(shiftProgrammedId, that.shiftProgrammedId) && Objects.equals(userId, that.userId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(shiftProgrammedId, userId);
    }
}
