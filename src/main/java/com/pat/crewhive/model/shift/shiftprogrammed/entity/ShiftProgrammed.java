package com.pat.crewhive.model.shift.shiftprogrammed.entity;

import com.pat.crewhive.model.shift.shiftprogrammed.ShiftUser;
import com.pat.crewhive.model.user.entity.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

@NoArgsConstructor
@Getter
@Setter
@Entity
@Table(name = "shift_programmed", indexes = {
        @Index(name = "idx_shiftprogrammed_user_id", columnList = "user_id"),
        @Index(name = "idx_shiftprogrammed_date", columnList = "shift_date"),
        @Index(name = "idx_shiftprogrammed", columnList = "start_shift")
})
public class ShiftProgrammed {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "shift_programmed_id", nullable = false)
    @Setter(AccessLevel.NONE)
    private Long shiftProgrammedId;

    @Version
    @Column(name = "version", nullable = false)
    @Setter(AccessLevel.NONE)
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
