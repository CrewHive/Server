package com.pat.crewhive.shiftprogrammed;

import com.pat.crewhive.common.audit.SoftDeletableEntity;
import com.pat.crewhive.user.User;
import jakarta.persistence.*;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

@Entity
@Table(name = "shift_user", indexes = {
        @Index(name = "idx_shiftuser", columnList = "shift_programmed_id"),
        @Index(name = "idx_shiftuser_user_id", columnList = "user_id"),
        @Index(name = "idx_shiftuser_active", columnList = "active"),
        @Index(name = "idx_shiftuser_deleted_at", columnList = "deleted_at"),
        @Index(name = "idx_shiftuser_deleted_by", columnList = "deleted_by")
}, uniqueConstraints = {
        @UniqueConstraint(name = "uc_shiftuser", columnNames = {"shift_programmed_id", "user_id"})
})
@SQLRestriction("active = true")
@SQLDelete(sql = "UPDATE shift_user SET active = false, deleted_at = now() WHERE shift_programmed_id = ? AND user_id = ?")
public class ShiftUser extends SoftDeletableEntity {

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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ShiftUser other)) return false;
        // A transient instance (either side, before @MapsId populates the id at flush)
        // is only equal to itself - falling through to id.equals() here would let two
        // distinct freshly-built links collide in a HashSet, silently dropping one.
        if (id == null || id.getShiftProgrammedId() == null || id.getUserId() == null) return false;
        if (other.id == null || other.id.getShiftProgrammedId() == null || other.id.getUserId() == null) return false;
        return id.equals(other.id);
    }

    @Override
    public int hashCode() {
        return 31;
    }
}
