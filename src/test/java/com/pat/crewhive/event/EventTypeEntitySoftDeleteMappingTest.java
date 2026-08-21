package com.pat.crewhive.event;

import com.pat.crewhive.common.audit.SoftDeletableEntity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

class EventTypeEntitySoftDeleteMappingTest {

    @Test
    void extendsSoftDeletableEntity() {
        assertThat(SoftDeletableEntity.class).isAssignableFrom(EventTypeEntity.class);
    }

    @Test
    void hasActiveOnlyRestriction() {
        SQLRestriction restriction = EventTypeEntity.class.getAnnotation(SQLRestriction.class);

        assertThat(restriction).isNotNull();
        assertThat(restriction.value()).isEqualTo("active = true");
    }

    @Test
    void softDeletesInsteadOfPhysicalDelete() {
        SQLDelete sqlDelete = EventTypeEntity.class.getAnnotation(SQLDelete.class);

        assertThat(sqlDelete).isNotNull();
        assertThat(sqlDelete.sql())
                .isEqualTo("UPDATE event_type SET active = false, deleted_at = now() WHERE id = ?");
    }

    @Test
    void hasAuditIndexes() {
        Index[] indexes = EventTypeEntity.class.getAnnotation(Table.class).indexes();
        Set<String> names = Arrays.stream(indexes).map(Index::name).collect(Collectors.toSet());

        assertThat(names).contains(
                "idx_event_type_active", "idx_event_type_deleted_at", "idx_event_type_deleted_by");
    }
}
