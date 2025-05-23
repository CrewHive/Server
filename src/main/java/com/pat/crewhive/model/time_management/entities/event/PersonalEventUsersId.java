package com.pat.crewhive.model.time_management.entities.event;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;
import java.util.Objects;

@Embeddable
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class PersonalEventUsersId implements Serializable {

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "personal_event_id", nullable = false)
    private Long eventId;

    @Override
    public boolean equals(Object o) {

        if(this == o) return true;
        if(!(o instanceof PersonalEventUsersId that)) return false;

        return Objects.equals(userId, that.userId) && Objects.equals(eventId, that.eventId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(userId, eventId);
    }
}
