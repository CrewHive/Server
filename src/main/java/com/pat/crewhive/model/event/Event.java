package com.pat.crewhive.model.event;


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
@Table(name = "event", indexes = {
        @Index(name = "idx_event_start_event", columnList = "start_event"),
        @Index(name = "idx_event_end_event", columnList = "end_event"),
        @Index(name = "idx_event_date", columnList = "date")
})

public class Event {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "event_id", nullable = false)
    @Setter(AccessLevel.NONE)
    private Long eventId;

    @Column(name = "name", nullable = false)
    private String eventName;

    @Column(name = "description")
    private String description;

    @Column(name = "start_event", nullable = false)
    private OffsetDateTime start;

    @Column(name = "end_event", nullable = false)
    private OffsetDateTime end;

    @Column(name = "date", nullable = false)
    private LocalDate date;

    @Column(name = "color", nullable = false)
    private String color;

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false)
    private EventType eventType;

    @OneToMany(mappedBy = "event", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<EventUsers> users = new LinkedHashSet<>();

    /**
     *
     *  Quando viene creato un evento viene chiamata questa funzione
     *  per aggiungere gli utenti selezionati
     */
    private void addUser(User u) {
        EventUsers link = new EventUsers(u, this);
        this.users.add(link);
        u.getPersonalEvents().add(link);
    }

    private void removeUser(User u) {
        this.users.removeIf(link -> link.getUser().equals(u));
        u.getPersonalEvents().removeIf(link -> link.getEvent().equals(this));
    }


    public Event(Set<User> user,
                         String name,
                         String description,
                         OffsetDateTime startEvent, OffsetDateTime endEvent,
                         String color,
                         EventType eventType) {

        for (User u : user) {
            addUser(u);
        }
        this.eventName = name;
        this.description = description;
        this.start = startEvent;
        this.end = endEvent;
        this.color = color;
        this.eventType = eventType;
        syncDate();
    }

    @PrePersist
    @PreUpdate
    private void syncDate() {
        this.date = start.toLocalDate();
    }

}
