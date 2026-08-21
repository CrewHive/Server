package com.pat.crewhive.shiftprogrammed;

import com.pat.crewhive.user.User;
import jakarta.persistence.*;

@Entity
@Table(name = "shift_user", indexes = {
        @Index(name = "idx_shiftuser", columnList = "shift_programmed_id"),
        @Index(name = "idx_shiftuser_user_id", columnList = "user_id")
}, uniqueConstraints = {
        @UniqueConstraint(name = "uc_shiftuser", columnNames = {"shift_programmed_id", "user_id"})
})
public class ShiftUser {

    @EmbeddedId
    private ShiftUserId id = new ShiftUserId();

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "shift_programmed_id", nullable = false)
    @MapsId("shiftProgrammedId")
    private ShiftProgrammed shift;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @MapsId("userId")
    private User user;

    public ShiftUser() {
    }

    public ShiftUser(ShiftProgrammed shift, User user) {
        this.shift = shift;
        this.user = user;
    }

    public ShiftUserId getId() {
        return id;
    }

    public void setId(ShiftUserId id) {
        this.id = id;
    }

    public ShiftProgrammed getShift() {
        return shift;
    }

    public void setShift(ShiftProgrammed shift) {
        this.shift = shift;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }
}
