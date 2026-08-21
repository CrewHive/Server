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
