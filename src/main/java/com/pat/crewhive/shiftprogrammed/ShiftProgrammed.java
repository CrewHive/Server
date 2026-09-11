package com.pat.crewhive.shiftprogrammed;

import com.pat.crewhive.common.shift.Shift;
import com.pat.crewhive.user.User;
import jakarta.persistence.*;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "shift_programmed", indexes = {
        @Index(name = "idx_shiftprogrammed_company_id", columnList = "company_id"),
        @Index(name = "idx_shiftprogrammed_date", columnList = "shift_date"),
        @Index(name = "idx_shiftprogrammed_start", columnList = "start_shift"),
        @Index(name = "idx_shiftprogrammed_active", columnList = "active"),
        @Index(name = "idx_shiftprogrammed_deleted_at", columnList = "deleted_at"),
        @Index(name = "idx_shiftprogrammed_deleted_by", columnList = "deleted_by")
})
@AttributeOverrides({
        @AttributeOverride(name = "id", column = @Column(name = "shift_programmed_id", nullable = false)),
        @AttributeOverride(name = "shiftName", column = @Column(name = "shift_programmed_name", nullable = false))
})
@SQLRestriction("active = true")
@SQLDelete(sql = "UPDATE shift_programmed SET active = false, deleted_at = now() WHERE shift_programmed_id = ? AND version = ?")
public class ShiftProgrammed extends Shift {

    @Column(name = "description")
    private String description;

    @Column(name = "color", nullable = false)
    private String color;

    @OneToMany(mappedBy = "shift", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<ShiftUser> users = new HashSet<>();

    public ShiftProgrammed() {
    }

    public UUID getShiftProgrammedId() {
        return getId();
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getColor() {
        return color;
    }

    public void setColor(String color) {
        this.color = color;
    }

    public Set<ShiftUser> getUsers() {
        return users;
    }

    public void setUsers(Set<ShiftUser> users) {
        this.users = users;
    }

    public void addUser(User u) {

        boolean alreadyPresent = this.users.stream()
                .anyMatch(eu -> Objects.equals(eu.getUser().getUserId(), u.getUserId()));

        if (!alreadyPresent) {
            ShiftUser link = new ShiftUser(this, u);
            this.users.add(link);
            u.getShiftUsers().add(link);
        }
    }

    /**
     * Collega uno ShiftUser già risolto (nuovo, oppure una riga soft-deleted
     * riattivata dal chiamante) a entrambi i lati dell'associazione. A differenza di
     * {@link #addUser}, non verifica duplicati né costruisce il link: quella
     * decisione richiede una query sul repository (per trovare una riga soft-deleted
     * che occupa già la stessa chiave primaria), possibile solo nel service layer.
     */
    public void attachUser(ShiftUser link) {
        this.users.add(link);
        link.getUser().getShiftUsers().add(link);
    }

    public void removeUser(User u) {

        this.users.removeIf(link -> {
            if (Objects.equals(link.getUser().getUserId(), u.getUserId())) {
                u.getShiftUsers().remove(link);
                link.setUser(null);
                link.setShift(null);
                return true;
            }
            return false;
        });
    }

}
