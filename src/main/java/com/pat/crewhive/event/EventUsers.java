package com.pat.crewhive.event;


import com.pat.crewhive.common.audit.SoftDeletableEntity;
import com.pat.crewhive.user.User;
import jakarta.persistence.*;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

@Entity
@Table(name = "event_users", indexes = {
        @Index(name = "idx_event_users_user_id", columnList = "user_id"),
        @Index(name = "idx_event_users_event_id", columnList = "event_id"),
        @Index(name = "idx_event_users_active", columnList = "active"),
        @Index(name = "idx_event_users_deleted_at", columnList = "deleted_at"),
        @Index(name = "idx_event_users_deleted_by", columnList = "deleted_by")
}, uniqueConstraints = {
        @UniqueConstraint(name = "uc_eventusers_event_id", columnNames = {"event_id", "user_id"})
})
@SQLRestriction("active = true")
@SQLDelete(sql = "UPDATE event_users SET active = false, deleted_at = now() WHERE user_id = ? AND event_id = ?")
public class EventUsers extends SoftDeletableEntity {

    @EmbeddedId
    private EventUsersId id = new EventUsersId();

    @ManyToOne (optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @MapsId("userId")
    private User user;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "event_id", nullable = false)
    @MapsId("eventId")
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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof EventUsers other)) return false;
        if (id == null || id.getUserId() == null || id.getEventId() == null) return false;
        if (other.id == null || other.id.getUserId() == null || other.id.getEventId() == null) return false;
        return id.equals(other.id);
    }

    @Override
    public int hashCode() {
        return 31;
    }

}
