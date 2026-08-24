package com.pat.crewhive.manager;

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

class UserRoleSoftDeleteMappingTest {

    @Test
    void extendsSoftDeletableEntity() {
        assertThat(SoftDeletableEntity.class).isAssignableFrom(UserRole.class);
    }

    @Test
    void hasActiveOnlyRestriction() {
        SQLRestriction restriction = UserRole.class.getAnnotation(SQLRestriction.class);

        assertThat(restriction).isNotNull();
        assertThat(restriction.value()).isEqualTo("active = true");
    }

    @Test
    void softDeletesInsteadOfPhysicalDelete() {
        SQLDelete sqlDelete = UserRole.class.getAnnotation(SQLDelete.class);

        assertThat(sqlDelete).isNotNull();
        assertThat(sqlDelete.sql())
                .isEqualTo("UPDATE user_role SET active = false, deleted_at = now() WHERE user_id = ? AND role_id = ?");
    }

    @Test
    void hasAuditIndexes() {
        Index[] indexes = UserRole.class.getAnnotation(Table.class).indexes();
        Set<String> names = Arrays.stream(indexes).map(Index::name).collect(Collectors.toSet());

        assertThat(names).contains(
                "idx_userrole_active", "idx_userrole_deleted_at", "idx_userrole_deleted_by");
    }
}
