package com.pat.hours_calculator.model.time_management.entities.event;


import com.pat.hours_calculator.model.user.entity.User;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(name = "personal_event_users",
        indexes = {
                @Index(name = "idx_personalevent_users_user_id", columnList = "user_id"),
                @Index(name = "idx_personalevent_users_personal_event_id", columnList = "personal_event_id")
        }
        )
public class PersonalEventUsers {

    @EmbeddedId
    private PersonalEventUsersId id;

    @ManyToOne (optional = false, fetch = FetchType.LAZY) @MapsId("userId")
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(optional = false, fetch = FetchType.LAZY) @MapsId("eventId")
    @JoinColumn(name = "personal_event_id", nullable = false)
    private PersonalEvent personalEvent;

    public PersonalEventUsers(User user, PersonalEvent personalEvent) {
        this.user = user;
        this.personalEvent = personalEvent;
        this.id = new PersonalEventUsersId(user.getUserId(), personalEvent.getId());
    }

}
