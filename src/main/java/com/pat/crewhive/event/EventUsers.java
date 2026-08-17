package com.pat.crewhive.event;


import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.pat.crewhive.user.User;
import jakarta.persistence.*;

@Entity
@Table(name = "event_users", indexes = {
        @Index(name = "idx_event_users_user_id", columnList = "user_id"),
        @Index(name = "idx_event_users_event_id", columnList = "event_id")
}, uniqueConstraints = {
        @UniqueConstraint(name = "uc_eventusers_event_id", columnNames = {"event_id", "user_id"})
})
public class EventUsers {
    //todo togli annotazioni json
    @EmbeddedId
    private EventUsersId id = new EventUsersId();

    @ManyToOne (optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @MapsId("userId")
    @JsonBackReference
    private User user;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "event_id", nullable = false)
    @MapsId("eventId")
    @JsonBackReference
    private Event event;

    public EventUsers() {
    }

    public EventUsers(User user, Event personalEvent) {
        this.user = user;
        this.event = personalEvent;
    }

    public EventUsersId getId() {
        return id;
    }

    public void setId(EventUsersId id) {
        this.id = id;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public Event getEvent() {
        return event;
    }

    public void setEvent(Event event) {
        this.event = event;
    }

}
