package com.pat.crewhive.shiftprogrammed;

import com.pat.crewhive.common.audit.SoftDeletableEntity;
import com.pat.crewhive.user.User;
import jakarta.persistence.*;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "shift_programmed", indexes = {
        @Index(name = "idx_shiftprogrammed_date", columnList = "shift_date"),
        @Index(name = "idx_shiftprogrammed_start", columnList = "start_shift"),
        @Index(name = "idx_shiftprogrammed_active", columnList = "active"),
        @Index(name = "idx_shiftprogrammed_deleted_at", columnList = "deleted_at"),
        @Index(name = "idx_shiftprogrammed_deleted_by", columnList = "deleted_by")
})
@SQLRestriction("active = true")
@SQLDelete(sql = "UPDATE shift_programmed SET active = false, deleted_at = now() WHERE shift_programmed_id = ?")
public class ShiftProgrammed extends SoftDeletableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "shift_programmed_id", nullable = false)
    private UUID shiftProgrammedId;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

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

    @OneToMany(mappedBy = "shift", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<ShiftUser> users = new HashSet<>();

    public ShiftProgrammed() {
    }

    public UUID getShiftProgrammedId() {
        return shiftProgrammedId;
    }

    public Long getVersion() {
        return version;
    }

    public String getShiftName() {
        return shiftName;
    }

    public void setShiftName(String shiftName) {
        this.shiftName = shiftName;
    }

    public OffsetDateTime getStart() {
        return start;
    }

    public void setStart(OffsetDateTime start) {
        this.start = start;
    }

    public OffsetDateTime getEnd() {
        return end;
    }

    public void setEnd(OffsetDateTime end) {
        this.end = end;
    }

    public LocalDate getDate() {
        return date;
    }

    public void setDate(LocalDate date) {
        this.date = date;
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

    public ShiftProgrammed(Set<User> user,
                 String name,
                 String description,
                 OffsetDateTime startEvent, OffsetDateTime endEvent,
                 String color) {

        for (User u : user) {
            addUser(u);
        }
        this.shiftName = name;
        this.description = description;
        this.start = startEvent;
        this.end = endEvent;
        this.color = color;
        syncDate();
    }

    @PrePersist
    @PreUpdate
    private void syncDate() {
        this.date = this.start.toLocalDate();
    }


}
