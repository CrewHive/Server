package com.pat.crewhive.common.audit;

import com.pat.crewhive.user.User;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class SoftDeletableEntityTest {

    private static class TestEntity extends SoftDeletableEntity {
    }

    @Test
    void newEntityIsActiveByDefault() {
        TestEntity entity = new TestEntity();

        assertThat(entity.isActive()).isTrue();
        assertThat(entity.getDeletedAt()).isNull();
        assertThat(entity.getDeletedBy()).isNull();
    }

    @Test
    void markDeletedSetsActiveFalseAndRecordsActorAndTimestamp() {
        TestEntity entity = new TestEntity();
        User actor = new User("actor@example.com", "Actor", "Name", "hash");

        entity.markDeleted(actor);

        assertThat(entity.isActive()).isFalse();
        assertThat(entity.getDeletedBy()).isSameAs(actor);
        assertThat(entity.getDeletedAt()).isNotNull();
        assertThat(entity.getDeletedAt()).isBeforeOrEqualTo(OffsetDateTime.now());
    }

    @Test
    void restoreClearsDeletionState() {
        TestEntity entity = new TestEntity();
        User actor = new User("actor@example.com", "Actor", "Name", "hash");
        entity.markDeleted(actor);

        entity.restore();

        assertThat(entity.isActive()).isTrue();
        assertThat(entity.getDeletedAt()).isNull();
        assertThat(entity.getDeletedBy()).isNull();
    }
}
