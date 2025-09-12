package com.pat.crewhive.model.event;


import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.pat.crewhive.model.user.entity.User;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter
@Setter
@NoArgsConstructor
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

    public EventUsers(User user, Event personalEvent) {
        this.user = user;
        this.event = personalEvent;
    }

}
