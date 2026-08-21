package com.pat.crewhive.company;

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

class CompanySoftDeleteMappingTest {

    @Test
    void extendsSoftDeletableEntity() {
        assertThat(SoftDeletableEntity.class).isAssignableFrom(Company.class);
    }

    @Test
    void hasActiveOnlyRestriction() {
        SQLRestriction restriction = Company.class.getAnnotation(SQLRestriction.class);

        assertThat(restriction).isNotNull();
        assertThat(restriction.value()).isEqualTo("active = true");
    }

    @Test
    void softDeletesInsteadOfPhysicalDelete() {
        SQLDelete sqlDelete = Company.class.getAnnotation(SQLDelete.class);

        assertThat(sqlDelete).isNotNull();
        assertThat(sqlDelete.sql())
                .isEqualTo("UPDATE company SET active = false, deleted_at = now() WHERE company_id = ?");
    }

    @Test
    void hasAuditIndexes() {
        Index[] indexes = Company.class.getAnnotation(Table.class).indexes();
        Set<String> names = Arrays.stream(indexes).map(Index::name).collect(Collectors.toSet());

        assertThat(names).contains(
                "idx_company_active", "idx_company_deleted_at", "idx_company_deleted_by");
    }
}
