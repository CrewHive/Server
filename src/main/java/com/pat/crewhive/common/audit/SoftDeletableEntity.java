package com.pat.crewhive.common.audit;

import com.pat.crewhive.user.User;
import jakarta.persistence.Column;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MappedSuperclass;

import java.time.OffsetDateTime;

/**
 * Campi di soft-delete comuni a tutte le entità di dominio che devono conservare
 * uno storico per audit/legal: nessuna riga viene mai cancellata fisicamente,
 * viene marcata inattiva con tracciamento di quando e da chi.
 */
@MappedSuperclass
public abstract class SoftDeletableEntity {

    @Column(name = "active", nullable = false)
    private boolean active = true;

    @Column(name = "deleted_at")
    private OffsetDateTime deletedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "deleted_by")
    private User deletedBy;

    public boolean isActive() {
        return active;
    }

    public OffsetDateTime getDeletedAt() {
        return deletedAt;
    }

    /**
     * The user who performed the deletion, if any. May throw when accessed if the
     * recorded actor has since been deactivated (including the common case where a
     * user deletes their own account: {@code deletedBy} then points at a row that is
     * itself {@code active = false}) - this is a {@code @ManyToOne(fetch = LAZY)} onto
     * {@link com.pat.crewhive.user.User}, which is itself {@code @SQLRestriction}ed, so
     * lazily resolving a reference to a now-hidden row fails instead of returning it.
     * Known, accepted limitation: reading the historical actor once they're deactivated
     * needs a query that bypasses {@code User}'s restriction (a native query, similar to
     * {@code UserRepository.existsInactiveByEmail}), not a plain lazy association read.
     */
    public User getDeletedBy() {
        return deletedBy;
    }

    /**
     * Marca l'entità come cancellata, tracciando l'attore. {@code actor} può essere
     * {@code null} quando la cancellazione è conseguenza di una cascata strutturale
     * (non un'azione diretta di un utente).
     */
    public void markDeleted(User actor) {
        this.active = false;
        this.deletedAt = OffsetDateTime.now();
        this.deletedBy = actor;
    }

    /** Ripristina l'entità a stato attivo, azzerando lo stato di cancellazione. */
    public void restore() {
        this.active = true;
        this.deletedAt = null;
        this.deletedBy = null;
    }
}
