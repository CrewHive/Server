package com.pat.crewhive.common.shift;

import com.pat.crewhive.common.audit.SoftDeletableEntity;
import com.pat.crewhive.company.Company;
import jakarta.persistence.Column;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Version;
import jakarta.validation.constraints.AssertTrue;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Campi comuni a tutte le entità "turno" del dominio ({@code ShiftWorked},
 * {@code ShiftProgrammed}): identità, versionamento ottimistico, nome, intervallo
 * start/end, la data (derivata da {@code start}) e la company a cui il turno
 * appartiene, valorizzata come snapshot al momento della creazione.
 * <p>
 * {@code id} e {@code shiftName} non dichiarano una colonna esplicita qui perché il
 * nome colonna differisce tra le sottoclassi esistenti (es. {@code shift_worked_id}
 * vs {@code shift_programmed_id}): va fissato per sottoclasse con
 * {@code @AttributeOverride}.
 */
@MappedSuperclass
public abstract class Shift extends SoftDeletableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Version
    private Long version;

    private String shiftName;

    @Column(name = "start_shift", nullable = false)
    private OffsetDateTime start;

    @Column(name = "end_shift", nullable = false)
    private OffsetDateTime end;

    @Column(name = "shift_date", nullable = false)
    private LocalDate date;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "company_id", nullable = false)
    private Company company;

    protected Shift() {
    }

    public UUID getId() {
        return id;
    }

    public Long getVersion() {
        return version;
    }

    public String getShiftName() {
        return shiftName;
    }

    public void setShiftName(String shiftName) {
        this.shiftName = shiftName;
    }

    public OffsetDateTime getStart() {
        return start;
    }

    public void setStart(OffsetDateTime start) {
        this.start = start;
    }

    public OffsetDateTime getEnd() {
        return end;
    }

    public void setEnd(OffsetDateTime end) {
        this.end = end;
    }

    public LocalDate getDate() {
        return date;
    }

    public void setDate(LocalDate date) {
        this.date = date;
    }

    public Company getCompany() {
        return company;
    }

    public void setCompany(Company company) {
        this.company = company;
    }

    @PrePersist
    @PreUpdate
    private void syncDate() {
        if (this.start != null) {
            this.date = this.start.toLocalDate();
        }
    }

    // Validazione bean: utile con @Valid sul DTO/Controller
    @AssertTrue(message = "La fine turno deve essere successiva all'inizio turno")
    private boolean isChronologicallyValid() {
        return start != null && end != null && end.isAfter(start);
    }
}
