package com.pat.crewhive.event;


import com.pat.crewhive.user.User;
import jakarta.persistence.*;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "event", indexes = {
        @Index(name = "idx_event_start_event", columnList = "start_event"),
        @Index(name = "idx_event_end_event", columnList = "end_event"),
        @Index(name = "idx_event_date", columnList = "date")
})
public class Event {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "event_id", nullable = false)
    private UUID eventId;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

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

    @OneToOne(optional = false)
    @JoinColumn(name = "event_type_id", nullable = false)
    private EventTypeEntity eventType;

    @OneToMany(mappedBy = "event", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<EventUsers> users = new HashSet<>();

    public Event() {
    }

    public UUID getEventId() {
        return eventId;
    }

    public Long getVersion() {
        return version;
    }

    public String getEventName() {
        return eventName;
    }

    public void setEventName(String eventName) {
        this.eventName = eventName;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
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

    public String getColor() {
        return color;
    }

    public void setColor(String color) {
        this.color = color;
    }

    public EventTypeEntity getEventType() {
        return eventType;
    }

    public void setEventType(EventTypeEntity eventType) {
        this.eventType = eventType;
    }

    public Set<EventUsers> getUsers() {
        return users;
    }

    public void setUsers(Set<EventUsers> users) {
        this.users = users;
    }

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

        this.users.removeIf(link -> {
            if (Objects.equals(link.getUser().getUserId(), u.getUserId())) {
                u.getPersonalEvents().remove(link);
                link.setUser(null);
                link.setEvent(null);
                return true;
            }
            return false;
        });
    }


    public Event(Set<User> user,
                         String name,
                         String description,
                         OffsetDateTime startEvent, OffsetDateTime endEvent,
                         String color,
                         EventTypeEntity eventType) {

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
