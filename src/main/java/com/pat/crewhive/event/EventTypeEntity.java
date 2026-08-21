package com.pat.crewhive.event;

import com.pat.crewhive.common.audit.SoftDeletableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

@Entity
@Table(name = "event_type", indexes = {
        @Index(name = "idx_event_type_active", columnList = "active"),
        @Index(name = "idx_event_type_deleted_at", columnList = "deleted_at"),
        @Index(name = "idx_event_type_deleted_by", columnList = "deleted_by")
})
@SQLRestriction("active = true")
@SQLDelete(sql = "UPDATE event_type SET active = false, deleted_at = now() WHERE id = ?")
public class EventTypeEntity extends SoftDeletableEntity {

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
