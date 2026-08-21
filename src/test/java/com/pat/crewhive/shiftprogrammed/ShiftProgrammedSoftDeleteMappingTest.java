package com.pat.crewhive.shiftprogrammed;

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

class ShiftProgrammedSoftDeleteMappingTest {

    @Test
    void extendsSoftDeletableEntity() {
        assertThat(SoftDeletableEntity.class).isAssignableFrom(ShiftProgrammed.class);
    }

    @Test
    void hasActiveOnlyRestriction() {
        SQLRestriction restriction = ShiftProgrammed.class.getAnnotation(SQLRestriction.class);

        assertThat(restriction).isNotNull();
        assertThat(restriction.value()).isEqualTo("active = true");
    }

    @Test
    void softDeletesInsteadOfPhysicalDelete() {
        SQLDelete sqlDelete = ShiftProgrammed.class.getAnnotation(SQLDelete.class);

        assertThat(sqlDelete).isNotNull();
        assertThat(sqlDelete.sql())
                .isEqualTo("UPDATE shift_programmed SET active = false, deleted_at = now() WHERE shift_programmed_id = ?");
    }

    @Test
    void hasAuditIndexes() {
        Index[] indexes = ShiftProgrammed.class.getAnnotation(Table.class).indexes();
        Set<String> names = Arrays.stream(indexes).map(Index::name).collect(Collectors.toSet());

        assertThat(names).contains(
                "idx_shiftprogrammed_active", "idx_shiftprogrammed_deleted_at", "idx_shiftprogrammed_deleted_by");
    }
}
