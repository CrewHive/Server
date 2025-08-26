package com.pat.crewhive.model.shift.shiftprogrammed;

import com.pat.crewhive.model.shift.shiftprogrammed.entity.ShiftProgrammed;
import com.pat.crewhive.model.user.entity.User;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "ShiftUser", indexes = {
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
    private ShiftUsersId id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "shift_programmed_id", nullable = false)
    @MapsId("shiftProgrammedId")
    private ShiftProgrammed shift;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @MapsId("userId")
    private User user;

    public ShiftUser(ShiftProgrammed shift, User user) {
        this.shift = shift;
        this.user = user;
    }
}
