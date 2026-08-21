package com.pat.crewhive.user;

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

class UserSoftDeleteMappingTest {

    @Test
    void extendsSoftDeletableEntity() {
        assertThat(SoftDeletableEntity.class).isAssignableFrom(User.class);
    }

    @Test
    void hasActiveOnlyRestriction() {
        SQLRestriction restriction = User.class.getAnnotation(SQLRestriction.class);

        assertThat(restriction).isNotNull();
        assertThat(restriction.value()).isEqualTo("active = true");
    }

    @Test
    void softDeletesInsteadOfPhysicalDelete() {
        SQLDelete sqlDelete = User.class.getAnnotation(SQLDelete.class);

        assertThat(sqlDelete).isNotNull();
        assertThat(sqlDelete.sql())
                .isEqualTo("UPDATE users SET active = false, deleted_at = now() WHERE user_id = ?");
    }

    @Test
    void hasAuditIndexes() {
        Index[] indexes = User.class.getAnnotation(Table.class).indexes();
        Set<String> names = Arrays.stream(indexes).map(Index::name).collect(Collectors.toSet());

        assertThat(names).contains(
                "idx_user_active", "idx_user_deleted_at", "idx_user_deleted_by");
    }
}
