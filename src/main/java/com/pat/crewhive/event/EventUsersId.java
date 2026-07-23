package com.pat.crewhive.event;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.*;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

@Embeddable
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@EqualsAndHashCode
public class EventUsersId implements Serializable {

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "event_id", nullable = false)
    private UUID eventId;
}
