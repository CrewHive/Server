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
import java.util.Objects;
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
     * Add a user to the event and the event to the user
     * @param u the user to add
     */
    public void addUser(User u) {

        boolean alreadyPresent = this.users.stream()
                .anyMatch(eu -> Objects.equals(eu.getUser().getUserId(), u.getUserId()));

        if (!alreadyPresent) {
            EventUsers link = new EventUsers(u, this);
            this.users.add(link);
            u.getPersonalEvents().add(link);
        }
    }


    /**
     * Remove the user from the event and the event from the user
     * @param u the user to remove
     */
    public void removeUser(User u) {

        Long uid = u.getUserId();

        this.users.removeIf(eu -> Objects.equals(eu.getUser().getUserId(), uid));

        u.getPersonalEvents().removeIf(eu -> Objects.equals(eu.getEvent().getEventId(), this.eventId));
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
        this.date = this.start.toLocalDate();
    }

}
