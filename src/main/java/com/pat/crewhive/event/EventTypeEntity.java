package com.pat.crewhive.event;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@Entity
@Table(name = "event_type")
public class EventTypeEntity {

    @Id
    private Short id;

    @Column(name = "name", nullable = false, unique = true)
    private String name;

    public EventTypeEntity(Short id, String name) {
        this.id = id;
        this.name = name;
    }
}
