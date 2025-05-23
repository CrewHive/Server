package com.pat.crewhive.model.time_management.entities.event;


import com.pat.crewhive.model.user.entity.User;
import com.pat.crewhive.model.util.EventType;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.LinkedHashSet;
import java.util.Set;

@NoArgsConstructor
@Getter
@Setter
@Entity
@Table(name = "personal_event", indexes = {
        @Index(name = "idx_personalevent_start_event", columnList = "start_event"),
        @Index(name = "idx_personalevent_end_event", columnList = "end_event"),
        @Index(name = "idx_personalevent_date", columnList = "date")
})

public class PersonalEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "personal_event_id", nullable = false)
    @Setter(AccessLevel.NONE)
    private Long id;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "description")
    private String description;

    @Column(name = "start_event", nullable = false)
    private OffsetDateTime startEvent;

    @Column(name = "end_event", nullable = false)
    private OffsetDateTime endEvent;

    @Column(name = "date", nullable = false)
    private LocalDate date;

    @Column(name = "color", nullable = false)
    private String color;

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false)
    private EventType eventType;

    @OneToMany(mappedBy = "personalEvent", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<PersonalEventUsers> eventUsers = new LinkedHashSet<>();


    private void addUser(User u) {
        PersonalEventUsers link = new PersonalEventUsers(u, this);
        this.eventUsers.add(link);
        u.getPersonalEventLinks().add(link);
    }

    private void removeUser(User u) {
        this.eventUsers.removeIf(link -> link.getUser().equals(u));
        u.getPersonalEventLinks().removeIf(link -> link.getPersonalEvent().equals(this));
    }


    public PersonalEvent(Set<User> user,
                         String name,
                         String description,
                         OffsetDateTime startEvent, OffsetDateTime endEvent,
                         String color,
                         EventType eventType) {

        for (User u : user) {
            addUser(u);
        }
        this.name = name;
        this.description = description;
        this.startEvent = startEvent;
        this.endEvent = endEvent;
        this.color = color;
        this.eventType = eventType;
        syncDate();
    }

    @PrePersist
    @PreUpdate
    private void syncDate() {
        this.date = startEvent.toLocalDate();
    }

}
