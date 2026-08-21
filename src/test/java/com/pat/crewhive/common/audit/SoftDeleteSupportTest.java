package com.pat.crewhive.common.audit;

import com.pat.crewhive.user.User;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SoftDeleteSupportTest {

    private static class TestEntity extends SoftDeletableEntity {
    }

    @Mock
    private JpaRepository<TestEntity, UUID> repository;

    @Test
    void softDeleteMarksEntityThenSavesThenDeletes() {
        TestEntity entity = new TestEntity();
        User actor = new User("actor@example.com", "Actor", "Name", "hash");

        when(repository.save(entity)).thenReturn(entity);

        SoftDeleteSupport.softDelete(repository, entity, actor);

        assertThat(entity.isActive()).isFalse();
        assertThat(entity.getDeletedBy()).isSameAs(actor);
        assertThat(entity.getDeletedAt()).isNotNull();

        // save() deve persistere deletedBy PRIMA che delete() faccia scattare la
        // cascata: @SQLDelete non può leggere il valore in memoria di quel campo.
        // delete() deve usare l'istanza restituita da save() (potenzialmente un'entità
        // gestita diversa, es. dopo un merge da uno stato detached), non l'originale.
        InOrder order = inOrder(repository);
        order.verify(repository).save(entity);
        order.verify(repository).delete(entity);
    }

    @Test
    void softDeleteUsesTheManagedInstanceReturnedBySaveForDelete() {
        TestEntity original = new TestEntity();
        TestEntity managed = new TestEntity();
        User actor = new User("actor@example.com", "Actor", "Name", "hash");
        when(repository.save(original)).thenReturn(managed);

        SoftDeleteSupport.softDelete(repository, original, actor);

        verify(repository).delete(managed);
        verify(repository, org.mockito.Mockito.never()).delete(original);
    }
}
