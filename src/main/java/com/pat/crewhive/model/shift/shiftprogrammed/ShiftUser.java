package com.pat.crewhive.model.shift.shiftprogrammed;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.pat.crewhive.model.shift.shiftprogrammed.entity.ShiftProgrammed;
import com.pat.crewhive.model.user.entity.User;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "shift_user", indexes = {
        @Index(name = "idx_shiftuser", columnList = "shift_programmed_id"),
        @Index(name = "idx_shiftuser_user_id", columnList = "user_id")
}, uniqueConstraints = {
        @UniqueConstraint(name = "uc_shiftuser", columnNames = {"shift_programmed_id", "user_id"})
})
@NoArgsConstructor
@Getter
@Setter
public class ShiftUser {

    @EmbeddedId
    private ShiftUserId id;

    //todo per il refactoring per la beta togliere jsonignore e comincia a creare i dto per i service e a modificare le qeury per dare le info che servono
    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "shift_programmed_id", nullable = false)
    @JsonIgnore
    @MapsId("shiftProgrammedId")
    private ShiftProgrammed shift;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @JsonIgnore
    @MapsId("userId")
    private User user;

    public ShiftUser(ShiftProgrammed shift, User user) {
        this.shift = shift;
        this.user = user;
    }

    @PrePersist
    void fillIdIfNull() {
        if (id == null && user != null && shift != null
                && user.getUserId() != null && shift.getShiftProgrammedId() != null) {
            this.id = new ShiftUserId(shift.getShiftProgrammedId(), user.getUserId());
        }
    }
}
