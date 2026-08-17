package com.pat.crewhive.event;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "event_type")
public class EventTypeEntity {

    @Id
    private Short id;

    @Column(name = "name", nullable = false, unique = true)
    private String name;

    public EventTypeEntity() {
    }

    public EventTypeEntity(Short id, String name) {
        this.id = id;
        this.name = name;
    }

    public Short getId() {
        return id;
    }

    public String getName() {
        return name;
    }
}
