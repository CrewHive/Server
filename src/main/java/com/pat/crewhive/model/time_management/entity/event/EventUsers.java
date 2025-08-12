package com.pat.crewhive.model.time_management.entity.event;


import com.pat.crewhive.model.user.entity.User;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(name = "event_users",
        indexes = {
                @Index(name = "idx_event_users_user_id", columnList = "user_id"),
                @Index(name = "idx_event_users_event_id", columnList = "event_id")
        }
        )
public class EventUsers {

    @EmbeddedId
    private EventUsersId id;

    @ManyToOne (optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @MapsId("userId")
    private User user;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "event_id", nullable = false)
    @MapsId("eventId")
    private Event event;

    public EventUsers(User user, Event personalEvent) {
        this.user = user;
        this.event = personalEvent;
        this.id = new EventUsersId(user.getUserId(), personalEvent.getEventId());
    }

}
