package com.pat.crewhive.common.audit;

import com.pat.crewhive.user.User;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Punto unico per le cancellazioni di primo livello: marca l'entità come cancellata
 * con l'attore che ha compiuto l'azione, persiste quello stato, poi la rimuove così
 * che la cascata JPA (cascade=ALL/orphanRemoval già presente sulle relazioni) raggiunga
 * i figli. Ogni entità coinvolta nella cascata deve avere {@code @SQLDelete} perché la
 * rimozione fisica diventi un UPDATE invece di una DELETE reale.
 */
public final class SoftDeleteSupport {

    private SoftDeleteSupport() {
    }

    public static <T extends SoftDeletableEntity, ID> void softDelete(JpaRepository<T, ID> repository, T entity, User actor) {

        entity.markDeleted(actor);
        T managed = repository.save(entity);
        repository.delete(managed);
    }
}
