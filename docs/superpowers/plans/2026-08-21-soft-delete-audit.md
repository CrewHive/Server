# Soft-delete audit su tutte le entità — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Sostituire ogni cancellazione fisica delle entità di dominio con un soft-delete uniforme (`active`, `deletedAt`, `deletedBy`), filtrato automaticamente dalle query e propagato a cascata dove già esiste `cascade=ALL`/`orphanRemoval=true`.

**Architecture:** Una `@MappedSuperclass` (`SoftDeletableEntity`) fornisce i tre campi a tutte le 11 entità in scope; `@SQLRestriction("active = true")` filtra automaticamente le letture; `@SQLDelete` trasforma ogni `EntityManager.remove()` (diretto o a cascata) in un `UPDATE`. Un helper (`SoftDeleteSupport.softDelete`) fa da punto unico per le cancellazioni di primo livello, così `deletedBy` viene sempre popolato correttamente prima che scatti la cascata.

**Tech Stack:** Spring Boot 4.1, Hibernate ORM (annotazioni `org.hibernate.annotations.SQLDelete`/`SQLRestriction`), Spring Data JPA, PostgreSQL, JUnit 5 + Mockito + AssertJ (nessun H2/Testcontainers nel progetto).

**Spec:** `docs/superpowers/specs/2026-08-21-soft-delete-audit-design.md`

## Global Constraints

- Entità in scope (ricevono i 3 campi): `Company`, `User`, `Event`, `EventUsers`, `Role`, `UserRole`, `ShiftProgrammed`, `ShiftUser`, `ShiftTemplate`, `ShiftWorked`, `EventTypeEntity`. Esclusa: `RefreshToken`.
- Nessuna riga viene mai cancellata fisicamente da codice applicativo: ogni cancellazione di primo livello passa da `SoftDeleteSupport.softDelete(repository, entity, actor)`, mai da `repository.delete(...)`/`deleteById(...)` diretta.
- `deletedBy` è popolato con l'attore reale solo sulle cancellazioni dirette di primo livello; sulle entità raggiunte per cascata resta `null` (cancellazione strutturale, non un'azione diretta).
- Niente Flyway/Liquibase, niente Testcontainers/H2: si resta su `ddl-auto=update` e sui test Mockito-only già in uso nel progetto (nessun test in questo piano richiede un database reale).
- Ogni entità in scope ottiene tre indici B-tree con naming `idx_<prefisso_tabella_esistente>_active|deleted_at|deleted_by`, appesi all'array `indexes` già presente (o creato ex-novo se assente) sul `@Table` dell'entità.
- Package nuovo: `com.pat.crewhive.common.audit` per `SoftDeletableEntity` e `SoftDeleteSupport`.

---

## Task 1: `SoftDeletableEntity` mapped superclass

**Files:**
- Create: `src/main/java/com/pat/crewhive/common/audit/SoftDeletableEntity.java`
- Test: `src/test/java/com/pat/crewhive/common/audit/SoftDeletableEntityTest.java`

**Interfaces:**
- Consumes: nulla (nuova classe base).
- Produces: `SoftDeletableEntity` con `isActive(): boolean`, `getDeletedAt(): OffsetDateTime`, `getDeletedBy(): User`, `markDeleted(User actor): void`, `restore(): void`. Ogni entità delle task successive estende questa classe.

- [ ] **Step 1: Scrivi il test che fallisce**

```java
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
```

- [ ] **Step 2: Esegui il test, verifica che fallisca**

Run: `./mvnw test -Dtest=SoftDeletableEntityTest`
Expected: FAIL (compilazione: `SoftDeletableEntity` non esiste ancora)

- [ ] **Step 3: Implementa la classe**

```java
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
```

- [ ] **Step 4: Esegui il test, verifica che passi**

Run: `./mvnw test -Dtest=SoftDeletableEntityTest`
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add src/main/java/com/pat/crewhive/common/audit/SoftDeletableEntity.java src/test/java/com/pat/crewhive/common/audit/SoftDeletableEntityTest.java
git commit -m "Aggiunge SoftDeletableEntity, base per il soft-delete di audit"
```

---

## Task 2: `SoftDeleteSupport` helper

**Files:**
- Create: `src/main/java/com/pat/crewhive/common/audit/SoftDeleteSupport.java`
- Test: `src/test/java/com/pat/crewhive/common/audit/SoftDeleteSupportTest.java`

**Interfaces:**
- Consumes: `SoftDeletableEntity` (Task 1).
- Produces: `SoftDeleteSupport.softDelete(JpaRepository<T, ID> repository, T entity, User actor): void`, usato da tutte le task 20-25 per le cancellazioni di primo livello.

- [ ] **Step 1: Scrivi il test che fallisce**

```java
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

        SoftDeleteSupport.softDelete(repository, entity, actor);

        assertThat(entity.isActive()).isFalse();
        assertThat(entity.getDeletedBy()).isSameAs(actor);
        assertThat(entity.getDeletedAt()).isNotNull();

        // save() deve persistere deletedBy PRIMA che delete() faccia scattare la
        // cascata: @SQLDelete non può leggere il valore in memoria di quel campo.
        InOrder order = inOrder(repository);
        order.verify(repository).save(entity);
        order.verify(repository).delete(entity);
    }
}
```

- [ ] **Step 2: Esegui il test, verifica che fallisca**

Run: `./mvnw test -Dtest=SoftDeleteSupportTest`
Expected: FAIL (compilazione: `SoftDeleteSupport` non esiste ancora)

- [ ] **Step 3: Implementa la classe**

```java
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

    public static <T extends SoftDeletableEntity, ID> void softDelete(
            JpaRepository<T, ID> repository, T entity, User actor) {

        entity.markDeleted(actor);
        repository.save(entity);
        repository.delete(entity);
    }
}
```

- [ ] **Step 4: Esegui il test, verifica che passi**

Run: `./mvnw test -Dtest=SoftDeleteSupportTest`
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add src/main/java/com/pat/crewhive/common/audit/SoftDeleteSupport.java src/test/java/com/pat/crewhive/common/audit/SoftDeleteSupportTest.java
git commit -m "Aggiunge SoftDeleteSupport per le cancellazioni di primo livello"
```

---

## Task 3-13: mapping soft-delete sulle 11 entità

Ogni task di questo gruppo segue lo stesso schema: l'entità estende `SoftDeletableEntity`
(Task 1), riceve `@SQLRestriction("active = true")` e `@SQLDelete(sql = "...")`, e tre
indici in più sul `@Table`. Il test è una verifica per riflessione (niente database:
controlla solo che le annotazioni Hibernate siano quelle attese), stesso schema per
tutte e 11: va scritto per intero in ogni task (non è importabile/condiviso) perché un
task successivo potrebbe essere letto/eseguito da un worker che non ha visto gli altri.

### Task 3: `EventTypeEntity`

**Files:**
- Modify: `src/main/java/com/pat/crewhive/event/EventTypeEntity.java`
- Test: `src/test/java/com/pat/crewhive/event/EventTypeEntitySoftDeleteMappingTest.java`

**Interfaces:**
- Consumes: `SoftDeletableEntity` (Task 1).
- Produces: nulla di nuovo per altre task (entità lookup, nessun collegamento cascata).

- [ ] **Step 1: Scrivi il test che fallisce**

```java
package com.pat.crewhive.event;

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

class EventTypeEntitySoftDeleteMappingTest {

    @Test
    void extendsSoftDeletableEntity() {
        assertThat(SoftDeletableEntity.class).isAssignableFrom(EventTypeEntity.class);
    }

    @Test
    void hasActiveOnlyRestriction() {
        SQLRestriction restriction = EventTypeEntity.class.getAnnotation(SQLRestriction.class);

        assertThat(restriction).isNotNull();
        assertThat(restriction.value()).isEqualTo("active = true");
    }

    @Test
    void softDeletesInsteadOfPhysicalDelete() {
        SQLDelete sqlDelete = EventTypeEntity.class.getAnnotation(SQLDelete.class);

        assertThat(sqlDelete).isNotNull();
        assertThat(sqlDelete.sql())
                .isEqualTo("UPDATE event_type SET active = false, deleted_at = now() WHERE id = ?");
    }

    @Test
    void hasAuditIndexes() {
        Index[] indexes = EventTypeEntity.class.getAnnotation(Table.class).indexes();
        Set<String> names = Arrays.stream(indexes).map(Index::name).collect(Collectors.toSet());

        assertThat(names).contains(
                "idx_event_type_active", "idx_event_type_deleted_at", "idx_event_type_deleted_by");
    }
}
```

- [ ] **Step 2: Esegui il test, verifica che fallisca**

Run: `./mvnw test -Dtest=EventTypeEntitySoftDeleteMappingTest`
Expected: FAIL (annotazioni assenti / classe non estende `SoftDeletableEntity`)

- [ ] **Step 3: Applica il mapping**

In `EventTypeEntity.java`, sostituisci:
```java
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "event_type")
public class EventTypeEntity {
```
con:
```java
import com.pat.crewhive.common.audit.SoftDeletableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

@Entity
@Table(name = "event_type", indexes = {
        @Index(name = "idx_event_type_active", columnList = "active"),
        @Index(name = "idx_event_type_deleted_at", columnList = "deleted_at"),
        @Index(name = "idx_event_type_deleted_by", columnList = "deleted_by")
})
@SQLRestriction("active = true")
@SQLDelete(sql = "UPDATE event_type SET active = false, deleted_at = now() WHERE id = ?")
public class EventTypeEntity extends SoftDeletableEntity {
```
(il resto della classe — campi, costruttori, getter — resta invariato).

- [ ] **Step 4: Esegui il test, verifica che passi**

Run: `./mvnw test -Dtest=EventTypeEntitySoftDeleteMappingTest`
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add src/main/java/com/pat/crewhive/event/EventTypeEntity.java src/test/java/com/pat/crewhive/event/EventTypeEntitySoftDeleteMappingTest.java
git commit -m "Applica il soft-delete a EventTypeEntity"
```

---

### Task 4: `Company`

**Files:**
- Modify: `src/main/java/com/pat/crewhive/company/Company.java`
- Test: `src/test/java/com/pat/crewhive/company/CompanySoftDeleteMappingTest.java`

**Interfaces:**
- Consumes: `SoftDeletableEntity` (Task 1).
- Produces: usata da Task 21 (`CompanyService.deleteCompany`).

- [ ] **Step 1: Scrivi il test che fallisce**

```java
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
```

- [ ] **Step 2: Esegui il test, verifica che fallisca**

Run: `./mvnw test -Dtest=CompanySoftDeleteMappingTest`
Expected: FAIL

- [ ] **Step 3: Applica il mapping**

In `Company.java`, sostituisci:
```java
import com.pat.crewhive.user.User;
import io.hypersistence.utils.hibernate.type.json.JsonType;
import jakarta.persistence.*;
import org.hibernate.annotations.Type;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "company")
public class Company {
```
con:
```java
import com.pat.crewhive.common.audit.SoftDeletableEntity;
import com.pat.crewhive.user.User;
import io.hypersistence.utils.hibernate.type.json.JsonType;
import jakarta.persistence.*;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;
import org.hibernate.annotations.Type;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "company", indexes = {
        @Index(name = "idx_company_active", columnList = "active"),
        @Index(name = "idx_company_deleted_at", columnList = "deleted_at"),
        @Index(name = "idx_company_deleted_by", columnList = "deleted_by")
})
@SQLRestriction("active = true")
@SQLDelete(sql = "UPDATE company SET active = false, deleted_at = now() WHERE company_id = ?")
public class Company extends SoftDeletableEntity {
```

- [ ] **Step 4: Esegui il test, verifica che passi**

Run: `./mvnw test -Dtest=CompanySoftDeleteMappingTest`
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add src/main/java/com/pat/crewhive/company/Company.java src/test/java/com/pat/crewhive/company/CompanySoftDeleteMappingTest.java
git commit -m "Applica il soft-delete a Company"
```

---

### Task 5: `ShiftTemplate`

**Files:**
- Modify: `src/main/java/com/pat/crewhive/shifttemplate/ShiftTemplate.java`
- Test: `src/test/java/com/pat/crewhive/shifttemplate/ShiftTemplateSoftDeleteMappingTest.java`

**Interfaces:**
- Consumes: `SoftDeletableEntity` (Task 1).
- Produces: usata da Task 23 (`ShiftTemplateService.deleteShiftTemplate`).

- [ ] **Step 1: Scrivi il test che fallisce**

```java
package com.pat.crewhive.shifttemplate;

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

class ShiftTemplateSoftDeleteMappingTest {

    @Test
    void extendsSoftDeletableEntity() {
        assertThat(SoftDeletableEntity.class).isAssignableFrom(ShiftTemplate.class);
    }

    @Test
    void hasActiveOnlyRestriction() {
        SQLRestriction restriction = ShiftTemplate.class.getAnnotation(SQLRestriction.class);

        assertThat(restriction).isNotNull();
        assertThat(restriction.value()).isEqualTo("active = true");
    }

    @Test
    void softDeletesInsteadOfPhysicalDelete() {
        SQLDelete sqlDelete = ShiftTemplate.class.getAnnotation(SQLDelete.class);

        assertThat(sqlDelete).isNotNull();
        assertThat(sqlDelete.sql())
                .isEqualTo("UPDATE shift_template SET active = false, deleted_at = now() WHERE shift_id = ?");
    }

    @Test
    void hasAuditIndexes() {
        Index[] indexes = ShiftTemplate.class.getAnnotation(Table.class).indexes();
        Set<String> names = Arrays.stream(indexes).map(Index::name).collect(Collectors.toSet());

        assertThat(names).contains(
                "idx_shifttemplate_active", "idx_shifttemplate_deleted_at", "idx_shifttemplate_deleted_by");
    }
}
```

- [ ] **Step 2: Esegui il test, verifica che fallisca**

Run: `./mvnw test -Dtest=ShiftTemplateSoftDeleteMappingTest`
Expected: FAIL

- [ ] **Step 3: Applica il mapping**

In `ShiftTemplate.java`, sostituisci:
```java
import com.pat.crewhive.company.Company;
import jakarta.persistence.*;

import java.time.OffsetTime;
import java.util.UUID;

@Entity
@Table(name = "shift_template", indexes = {
        @Index(name = "idx_shifttemplate_shift_name", columnList = "shift_name"),
        @Index(name = "idx_shifttemplate_company_id", columnList = "company_id")
}, uniqueConstraints = {
        @UniqueConstraint(name = "uk_shifttemplate_shift_name_company_id", columnNames = {"shift_name", "company_id"})
})
public class ShiftTemplate {
```
con:
```java
import com.pat.crewhive.common.audit.SoftDeletableEntity;
import com.pat.crewhive.company.Company;
import jakarta.persistence.*;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

import java.time.OffsetTime;
import java.util.UUID;

@Entity
@Table(name = "shift_template", indexes = {
        @Index(name = "idx_shifttemplate_shift_name", columnList = "shift_name"),
        @Index(name = "idx_shifttemplate_company_id", columnList = "company_id"),
        @Index(name = "idx_shifttemplate_active", columnList = "active"),
        @Index(name = "idx_shifttemplate_deleted_at", columnList = "deleted_at"),
        @Index(name = "idx_shifttemplate_deleted_by", columnList = "deleted_by")
}, uniqueConstraints = {
        @UniqueConstraint(name = "uk_shifttemplate_shift_name_company_id", columnNames = {"shift_name", "company_id"})
})
@SQLRestriction("active = true")
@SQLDelete(sql = "UPDATE shift_template SET active = false, deleted_at = now() WHERE shift_id = ?")
public class ShiftTemplate extends SoftDeletableEntity {
```

- [ ] **Step 4: Esegui il test, verifica che passi**

Run: `./mvnw test -Dtest=ShiftTemplateSoftDeleteMappingTest`
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add src/main/java/com/pat/crewhive/shifttemplate/ShiftTemplate.java src/test/java/com/pat/crewhive/shifttemplate/ShiftTemplateSoftDeleteMappingTest.java
git commit -m "Applica il soft-delete a ShiftTemplate"
```

---

### Task 6: `ShiftWorked`

**Files:**
- Modify: `src/main/java/com/pat/crewhive/shiftworked/ShiftWorked.java`
- Test: `src/test/java/com/pat/crewhive/shiftworked/ShiftWorkedSoftDeleteMappingTest.java`

**Interfaces:**
- Consumes: `SoftDeletableEntity` (Task 1).
- Produces: nessun call site di cancellazione esiste oggi per `ShiftWorked` (nessun `ShiftWorkedService.delete...`); il mapping resta pronto per quando verrà aggiunto.

- [ ] **Step 1: Scrivi il test che fallisce**

```java
package com.pat.crewhive.shiftworked;

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

class ShiftWorkedSoftDeleteMappingTest {

    @Test
    void extendsSoftDeletableEntity() {
        assertThat(SoftDeletableEntity.class).isAssignableFrom(ShiftWorked.class);
    }

    @Test
    void hasActiveOnlyRestriction() {
        SQLRestriction restriction = ShiftWorked.class.getAnnotation(SQLRestriction.class);

        assertThat(restriction).isNotNull();
        assertThat(restriction.value()).isEqualTo("active = true");
    }

    @Test
    void softDeletesInsteadOfPhysicalDelete() {
        SQLDelete sqlDelete = ShiftWorked.class.getAnnotation(SQLDelete.class);

        assertThat(sqlDelete).isNotNull();
        assertThat(sqlDelete.sql())
                .isEqualTo("UPDATE shift_worked SET active = false, deleted_at = now() WHERE shift_worked_id = ?");
    }

    @Test
    void hasAuditIndexes() {
        Index[] indexes = ShiftWorked.class.getAnnotation(Table.class).indexes();
        Set<String> names = Arrays.stream(indexes).map(Index::name).collect(Collectors.toSet());

        assertThat(names).contains(
                "idx_shift_worked_active", "idx_shift_worked_deleted_at", "idx_shift_worked_deleted_by");
    }
}
```

- [ ] **Step 2: Esegui il test, verifica che fallisca**

Run: `./mvnw test -Dtest=ShiftWorkedSoftDeleteMappingTest`
Expected: FAIL

- [ ] **Step 3: Applica il mapping**

In `ShiftWorked.java`, sostituisci:
```java
import com.pat.crewhive.user.User;
import jakarta.persistence.*;
import jakarta.validation.constraints.AssertTrue;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;


@Entity
@Table(name = "shift_worked", indexes = {
        @Index(name = "idx_shift_worked_user_id", columnList = "user_id"),
        @Index(name = "idx_shift_worked_start_shift", columnList = "start_shift"),
        @Index(name = "idx_shift_worked_end_shift", columnList = "end_shift"),
        @Index(name = "idx_shift_worked_date", columnList = "shift_date"),
})
public class ShiftWorked {
```
con:
```java
import com.pat.crewhive.common.audit.SoftDeletableEntity;
import com.pat.crewhive.user.User;
import jakarta.persistence.*;
import jakarta.validation.constraints.AssertTrue;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;


@Entity
@Table(name = "shift_worked", indexes = {
        @Index(name = "idx_shift_worked_user_id", columnList = "user_id"),
        @Index(name = "idx_shift_worked_start_shift", columnList = "start_shift"),
        @Index(name = "idx_shift_worked_end_shift", columnList = "end_shift"),
        @Index(name = "idx_shift_worked_date", columnList = "shift_date"),
        @Index(name = "idx_shift_worked_active", columnList = "active"),
        @Index(name = "idx_shift_worked_deleted_at", columnList = "deleted_at"),
        @Index(name = "idx_shift_worked_deleted_by", columnList = "deleted_by")
})
@SQLRestriction("active = true")
@SQLDelete(sql = "UPDATE shift_worked SET active = false, deleted_at = now() WHERE shift_worked_id = ?")
public class ShiftWorked extends SoftDeletableEntity {
```

- [ ] **Step 4: Esegui il test, verifica che passi**

Run: `./mvnw test -Dtest=ShiftWorkedSoftDeleteMappingTest`
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add src/main/java/com/pat/crewhive/shiftworked/ShiftWorked.java src/test/java/com/pat/crewhive/shiftworked/ShiftWorkedSoftDeleteMappingTest.java
git commit -m "Applica il soft-delete a ShiftWorked"
```

---

### Task 7: `Role`

**Files:**
- Modify: `src/main/java/com/pat/crewhive/manager/Role.java`
- Test: `src/test/java/com/pat/crewhive/manager/RoleSoftDeleteMappingTest.java`

**Interfaces:**
- Consumes: `SoftDeletableEntity` (Task 1).
- Produces: usata da Task 22 (`RoleService.deleteRole`); `Role.users` (`cascade=ALL, orphanRemoval=true` verso `UserRole`, Task 10) è il canale di cascata per le assegnazioni di ruolo quando un ruolo viene cancellato.

- [ ] **Step 1: Scrivi il test che fallisce**

```java
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

class RoleSoftDeleteMappingTest {

    @Test
    void extendsSoftDeletableEntity() {
        assertThat(SoftDeletableEntity.class).isAssignableFrom(Role.class);
    }

    @Test
    void hasActiveOnlyRestriction() {
        SQLRestriction restriction = Role.class.getAnnotation(SQLRestriction.class);

        assertThat(restriction).isNotNull();
        assertThat(restriction.value()).isEqualTo("active = true");
    }

    @Test
    void softDeletesInsteadOfPhysicalDelete() {
        SQLDelete sqlDelete = Role.class.getAnnotation(SQLDelete.class);

        assertThat(sqlDelete).isNotNull();
        assertThat(sqlDelete.sql())
                .isEqualTo("UPDATE role SET active = false, deleted_at = now() WHERE role_id = ?");
    }

    @Test
    void hasAuditIndexes() {
        Index[] indexes = Role.class.getAnnotation(Table.class).indexes();
        Set<String> names = Arrays.stream(indexes).map(Index::name).collect(Collectors.toSet());

        assertThat(names).contains(
                "idx_role_active", "idx_role_deleted_at", "idx_role_deleted_by");
    }
}
```

- [ ] **Step 2: Esegui il test, verifica che fallisca**

Run: `./mvnw test -Dtest=RoleSoftDeleteMappingTest`
Expected: FAIL

- [ ] **Step 3: Applica il mapping**

In `Role.java`, sostituisci:
```java
import com.pat.crewhive.company.Company;
import jakarta.persistence.*;

import java.util.LinkedHashSet;
import java.util.Set;

@Entity
@Table(name = "role", indexes = {
        @Index(name = "idx_role_company_id", columnList = "company_id")
}, uniqueConstraints = {
        @UniqueConstraint(name = "uc_role_role_name_company_id", columnNames = {"role_name", "company_id"})
})
public class Role {
```
con:
```java
import com.pat.crewhive.common.audit.SoftDeletableEntity;
import com.pat.crewhive.company.Company;
import jakarta.persistence.*;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

import java.util.LinkedHashSet;
import java.util.Set;

@Entity
@Table(name = "role", indexes = {
        @Index(name = "idx_role_company_id", columnList = "company_id"),
        @Index(name = "idx_role_active", columnList = "active"),
        @Index(name = "idx_role_deleted_at", columnList = "deleted_at"),
        @Index(name = "idx_role_deleted_by", columnList = "deleted_by")
}, uniqueConstraints = {
        @UniqueConstraint(name = "uc_role_role_name_company_id", columnNames = {"role_name", "company_id"})
})
@SQLRestriction("active = true")
@SQLDelete(sql = "UPDATE role SET active = false, deleted_at = now() WHERE role_id = ?")
public class Role extends SoftDeletableEntity {
```

- [ ] **Step 4: Esegui il test, verifica che passi**

Run: `./mvnw test -Dtest=RoleSoftDeleteMappingTest`
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add src/main/java/com/pat/crewhive/manager/Role.java src/test/java/com/pat/crewhive/manager/RoleSoftDeleteMappingTest.java
git commit -m "Applica il soft-delete a Role"
```

---

### Task 8: `ShiftProgrammed`

**Files:**
- Modify: `src/main/java/com/pat/crewhive/shiftprogrammed/ShiftProgrammed.java`
- Test: `src/test/java/com/pat/crewhive/shiftprogrammed/ShiftProgrammedSoftDeleteMappingTest.java`

**Interfaces:**
- Consumes: `SoftDeletableEntity` (Task 1).
- Produces: usata da Task 19 (reactivate-on-recreate) e Task 25 (`ShiftProgrammedService.deleteShift`).

- [ ] **Step 1: Scrivi il test che fallisce**

```java
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
```

- [ ] **Step 2: Esegui il test, verifica che fallisca**

Run: `./mvnw test -Dtest=ShiftProgrammedSoftDeleteMappingTest`
Expected: FAIL

- [ ] **Step 3: Applica il mapping**

In `ShiftProgrammed.java`, sostituisci:
```java
import com.pat.crewhive.user.User;
import jakarta.persistence.*;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "shift_programmed", indexes = {
        @Index(name = "idx_shiftprogrammed_date", columnList = "shift_date"),
        @Index(name = "idx_shiftprogrammed_start", columnList = "start_shift")
})
public class ShiftProgrammed {
```
con:
```java
import com.pat.crewhive.common.audit.SoftDeletableEntity;
import com.pat.crewhive.user.User;
import jakarta.persistence.*;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "shift_programmed", indexes = {
        @Index(name = "idx_shiftprogrammed_date", columnList = "shift_date"),
        @Index(name = "idx_shiftprogrammed_start", columnList = "start_shift"),
        @Index(name = "idx_shiftprogrammed_active", columnList = "active"),
        @Index(name = "idx_shiftprogrammed_deleted_at", columnList = "deleted_at"),
        @Index(name = "idx_shiftprogrammed_deleted_by", columnList = "deleted_by")
})
@SQLRestriction("active = true")
@SQLDelete(sql = "UPDATE shift_programmed SET active = false, deleted_at = now() WHERE shift_programmed_id = ?")
public class ShiftProgrammed extends SoftDeletableEntity {
```

- [ ] **Step 4: Esegui il test, verifica che passi**

Run: `./mvnw test -Dtest=ShiftProgrammedSoftDeleteMappingTest`
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add src/main/java/com/pat/crewhive/shiftprogrammed/ShiftProgrammed.java src/test/java/com/pat/crewhive/shiftprogrammed/ShiftProgrammedSoftDeleteMappingTest.java
git commit -m "Applica il soft-delete a ShiftProgrammed"
```

---

### Task 9: `Event`

**Files:**
- Modify: `src/main/java/com/pat/crewhive/event/Event.java`
- Test: `src/test/java/com/pat/crewhive/event/EventSoftDeleteMappingTest.java`

**Interfaces:**
- Consumes: `SoftDeletableEntity` (Task 1).
- Produces: usata da Task 18 (reactivate-on-recreate) e Task 24 (`EventService.deleteEvent`).

- [ ] **Step 1: Scrivi il test che fallisce**

```java
package com.pat.crewhive.event;

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

class EventSoftDeleteMappingTest {

    @Test
    void extendsSoftDeletableEntity() {
        assertThat(SoftDeletableEntity.class).isAssignableFrom(Event.class);
    }

    @Test
    void hasActiveOnlyRestriction() {
        SQLRestriction restriction = Event.class.getAnnotation(SQLRestriction.class);

        assertThat(restriction).isNotNull();
        assertThat(restriction.value()).isEqualTo("active = true");
    }

    @Test
    void softDeletesInsteadOfPhysicalDelete() {
        SQLDelete sqlDelete = Event.class.getAnnotation(SQLDelete.class);

        assertThat(sqlDelete).isNotNull();
        assertThat(sqlDelete.sql())
                .isEqualTo("UPDATE event SET active = false, deleted_at = now() WHERE event_id = ?");
    }

    @Test
    void hasAuditIndexes() {
        Index[] indexes = Event.class.getAnnotation(Table.class).indexes();
        Set<String> names = Arrays.stream(indexes).map(Index::name).collect(Collectors.toSet());

        assertThat(names).contains(
                "idx_event_active", "idx_event_deleted_at", "idx_event_deleted_by");
    }
}
```

- [ ] **Step 2: Esegui il test, verifica che fallisca**

Run: `./mvnw test -Dtest=EventSoftDeleteMappingTest`
Expected: FAIL

- [ ] **Step 3: Applica il mapping**

In `Event.java`, sostituisci:
```java
import com.pat.crewhive.user.User;
import jakarta.persistence.*;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "event", indexes = {
        @Index(name = "idx_event_start_event", columnList = "start_event"),
        @Index(name = "idx_event_end_event", columnList = "end_event"),
        @Index(name = "idx_event_date", columnList = "date")
})
public class Event {
```
con:
```java
import com.pat.crewhive.common.audit.SoftDeletableEntity;
import com.pat.crewhive.user.User;
import jakarta.persistence.*;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "event", indexes = {
        @Index(name = "idx_event_start_event", columnList = "start_event"),
        @Index(name = "idx_event_end_event", columnList = "end_event"),
        @Index(name = "idx_event_date", columnList = "date"),
        @Index(name = "idx_event_active", columnList = "active"),
        @Index(name = "idx_event_deleted_at", columnList = "deleted_at"),
        @Index(name = "idx_event_deleted_by", columnList = "deleted_by")
})
@SQLRestriction("active = true")
@SQLDelete(sql = "UPDATE event SET active = false, deleted_at = now() WHERE event_id = ?")
public class Event extends SoftDeletableEntity {
```

- [ ] **Step 4: Esegui il test, verifica che passi**

Run: `./mvnw test -Dtest=EventSoftDeleteMappingTest`
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add src/main/java/com/pat/crewhive/event/Event.java src/test/java/com/pat/crewhive/event/EventSoftDeleteMappingTest.java
git commit -m "Applica il soft-delete a Event"
```

---

### Task 10: `UserRole`

**Files:**
- Modify: `src/main/java/com/pat/crewhive/manager/UserRole.java`
- Test: `src/test/java/com/pat/crewhive/manager/UserRoleSoftDeleteMappingTest.java`

**Interfaces:**
- Consumes: `SoftDeletableEntity` (Task 1).
- Produces: raggiunta per cascata da `User.role` (Task 13) e `Role.users` (Task 7).

- [ ] **Step 1: Scrivi il test che fallisce**

```java
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
                .isEqualTo("UPDATE user_role SET active = false, deleted_at = now() WHERE user_id = ?");
    }

    @Test
    void hasAuditIndexes() {
        Index[] indexes = UserRole.class.getAnnotation(Table.class).indexes();
        Set<String> names = Arrays.stream(indexes).map(Index::name).collect(Collectors.toSet());

        assertThat(names).contains(
                "idx_userrole_active", "idx_userrole_deleted_at", "idx_userrole_deleted_by");
    }
}
```

- [ ] **Step 2: Esegui il test, verifica che fallisca**

Run: `./mvnw test -Dtest=UserRoleSoftDeleteMappingTest`
Expected: FAIL

- [ ] **Step 3: Applica il mapping**

In `UserRole.java`, sostituisci:
```java
import com.pat.crewhive.user.User;
import jakarta.persistence.*;

import java.util.UUID;


@Entity
@Table(name = "user_role", indexes = {
        @Index(name = "idx_userrole_user_id", columnList = "user_id"),
        @Index(name = "idx_userrole_role_id", columnList = "role_id")
})
public class UserRole {
```
con:
```java
import com.pat.crewhive.common.audit.SoftDeletableEntity;
import com.pat.crewhive.user.User;
import jakarta.persistence.*;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

import java.util.UUID;


@Entity
@Table(name = "user_role", indexes = {
        @Index(name = "idx_userrole_user_id", columnList = "user_id"),
        @Index(name = "idx_userrole_role_id", columnList = "role_id"),
        @Index(name = "idx_userrole_active", columnList = "active"),
        @Index(name = "idx_userrole_deleted_at", columnList = "deleted_at"),
        @Index(name = "idx_userrole_deleted_by", columnList = "deleted_by")
})
@SQLRestriction("active = true")
@SQLDelete(sql = "UPDATE user_role SET active = false, deleted_at = now() WHERE user_id = ?")
public class UserRole extends SoftDeletableEntity {
```
(l'`equals`/`hashCode` già presenti in fondo alla classe restano invariati).

- [ ] **Step 4: Esegui il test, verifica che passi**

Run: `./mvnw test -Dtest=UserRoleSoftDeleteMappingTest`
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add src/main/java/com/pat/crewhive/manager/UserRole.java src/test/java/com/pat/crewhive/manager/UserRoleSoftDeleteMappingTest.java
git commit -m "Applica il soft-delete a UserRole"
```

---

### Task 11: `EventUsers`

**Files:**
- Modify: `src/main/java/com/pat/crewhive/event/EventUsers.java`
- Test: `src/test/java/com/pat/crewhive/event/EventUsersSoftDeleteMappingTest.java`

**Interfaces:**
- Consumes: `SoftDeletableEntity` (Task 1).
- Produces: raggiunta per cascata da `User.personalEvents` e `Event.users` (Task 9). `@EmbeddedId` composito: `EventUsersId` dichiara `userId` poi `eventId`, quindi `@SQLDelete` lega i due `?` in quest'ordine.

- [ ] **Step 1: Scrivi il test che fallisce**

```java
package com.pat.crewhive.event;

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

class EventUsersSoftDeleteMappingTest {

    @Test
    void extendsSoftDeletableEntity() {
        assertThat(SoftDeletableEntity.class).isAssignableFrom(EventUsers.class);
    }

    @Test
    void hasActiveOnlyRestriction() {
        SQLRestriction restriction = EventUsers.class.getAnnotation(SQLRestriction.class);

        assertThat(restriction).isNotNull();
        assertThat(restriction.value()).isEqualTo("active = true");
    }

    @Test
    void softDeletesInsteadOfPhysicalDelete() {
        SQLDelete sqlDelete = EventUsers.class.getAnnotation(SQLDelete.class);

        assertThat(sqlDelete).isNotNull();
        assertThat(sqlDelete.sql())
                .isEqualTo("UPDATE event_users SET active = false, deleted_at = now() WHERE user_id = ? AND event_id = ?");
    }

    @Test
    void hasAuditIndexes() {
        Index[] indexes = EventUsers.class.getAnnotation(Table.class).indexes();
        Set<String> names = Arrays.stream(indexes).map(Index::name).collect(Collectors.toSet());

        assertThat(names).contains(
                "idx_event_users_active", "idx_event_users_deleted_at", "idx_event_users_deleted_by");
    }
}
```

- [ ] **Step 2: Esegui il test, verifica che fallisca**

Run: `./mvnw test -Dtest=EventUsersSoftDeleteMappingTest`
Expected: FAIL

- [ ] **Step 3: Applica il mapping**

In `EventUsers.java`, sostituisci:
```java
import com.pat.crewhive.user.User;
import jakarta.persistence.*;

@Entity
@Table(name = "event_users", indexes = {
        @Index(name = "idx_event_users_user_id", columnList = "user_id"),
        @Index(name = "idx_event_users_event_id", columnList = "event_id")
}, uniqueConstraints = {
        @UniqueConstraint(name = "uc_eventusers_event_id", columnNames = {"event_id", "user_id"})
})
public class EventUsers {
```
con:
```java
import com.pat.crewhive.common.audit.SoftDeletableEntity;
import com.pat.crewhive.user.User;
import jakarta.persistence.*;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

@Entity
@Table(name = "event_users", indexes = {
        @Index(name = "idx_event_users_user_id", columnList = "user_id"),
        @Index(name = "idx_event_users_event_id", columnList = "event_id"),
        @Index(name = "idx_event_users_active", columnList = "active"),
        @Index(name = "idx_event_users_deleted_at", columnList = "deleted_at"),
        @Index(name = "idx_event_users_deleted_by", columnList = "deleted_by")
}, uniqueConstraints = {
        @UniqueConstraint(name = "uc_eventusers_event_id", columnNames = {"event_id", "user_id"})
})
@SQLRestriction("active = true")
@SQLDelete(sql = "UPDATE event_users SET active = false, deleted_at = now() WHERE user_id = ? AND event_id = ?")
public class EventUsers extends SoftDeletableEntity {
```
(l'`equals`/`hashCode` già presenti restano invariati).

- [ ] **Step 4: Esegui il test, verifica che passi**

Run: `./mvnw test -Dtest=EventUsersSoftDeleteMappingTest`
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add src/main/java/com/pat/crewhive/event/EventUsers.java src/test/java/com/pat/crewhive/event/EventUsersSoftDeleteMappingTest.java
git commit -m "Applica il soft-delete a EventUsers"
```

---

### Task 12: `ShiftUser`

**Files:**
- Modify: `src/main/java/com/pat/crewhive/shiftprogrammed/ShiftUser.java`
- Test: `src/test/java/com/pat/crewhive/shiftprogrammed/ShiftUserSoftDeleteMappingTest.java`

**Interfaces:**
- Consumes: `SoftDeletableEntity` (Task 1).
- Produces: raggiunta per cascata da `User.shiftUsers` e `ShiftProgrammed.users` (Task 8). `@EmbeddedId` composito: `ShiftUserId` dichiara `shiftProgrammedId` poi `userId`.

- [ ] **Step 1: Scrivi il test che fallisce**

```java
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

class ShiftUserSoftDeleteMappingTest {

    @Test
    void extendsSoftDeletableEntity() {
        assertThat(SoftDeletableEntity.class).isAssignableFrom(ShiftUser.class);
    }

    @Test
    void hasActiveOnlyRestriction() {
        SQLRestriction restriction = ShiftUser.class.getAnnotation(SQLRestriction.class);

        assertThat(restriction).isNotNull();
        assertThat(restriction.value()).isEqualTo("active = true");
    }

    @Test
    void softDeletesInsteadOfPhysicalDelete() {
        SQLDelete sqlDelete = ShiftUser.class.getAnnotation(SQLDelete.class);

        assertThat(sqlDelete).isNotNull();
        assertThat(sqlDelete.sql())
                .isEqualTo("UPDATE shift_user SET active = false, deleted_at = now() WHERE shift_programmed_id = ? AND user_id = ?");
    }

    @Test
    void hasAuditIndexes() {
        Index[] indexes = ShiftUser.class.getAnnotation(Table.class).indexes();
        Set<String> names = Arrays.stream(indexes).map(Index::name).collect(Collectors.toSet());

        assertThat(names).contains(
                "idx_shiftuser_active", "idx_shiftuser_deleted_at", "idx_shiftuser_deleted_by");
    }
}
```

- [ ] **Step 2: Esegui il test, verifica che fallisca**

Run: `./mvnw test -Dtest=ShiftUserSoftDeleteMappingTest`
Expected: FAIL

- [ ] **Step 3: Applica il mapping**

In `ShiftUser.java`, sostituisci:
```java
import com.pat.crewhive.user.User;
import jakarta.persistence.*;

@Entity
@Table(name = "shift_user", indexes = {
        @Index(name = "idx_shiftuser", columnList = "shift_programmed_id"),
        @Index(name = "idx_shiftuser_user_id", columnList = "user_id")
}, uniqueConstraints = {
        @UniqueConstraint(name = "uc_shiftuser", columnNames = {"shift_programmed_id", "user_id"})
})
public class ShiftUser {
```
con:
```java
import com.pat.crewhive.common.audit.SoftDeletableEntity;
import com.pat.crewhive.user.User;
import jakarta.persistence.*;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

@Entity
@Table(name = "shift_user", indexes = {
        @Index(name = "idx_shiftuser", columnList = "shift_programmed_id"),
        @Index(name = "idx_shiftuser_user_id", columnList = "user_id"),
        @Index(name = "idx_shiftuser_active", columnList = "active"),
        @Index(name = "idx_shiftuser_deleted_at", columnList = "deleted_at"),
        @Index(name = "idx_shiftuser_deleted_by", columnList = "deleted_by")
}, uniqueConstraints = {
        @UniqueConstraint(name = "uc_shiftuser", columnNames = {"shift_programmed_id", "user_id"})
})
@SQLRestriction("active = true")
@SQLDelete(sql = "UPDATE shift_user SET active = false, deleted_at = now() WHERE shift_programmed_id = ? AND user_id = ?")
public class ShiftUser extends SoftDeletableEntity {
```
(l'`equals`/`hashCode` già presenti restano invariati).

- [ ] **Step 4: Esegui il test, verifica che passi**

Run: `./mvnw test -Dtest=ShiftUserSoftDeleteMappingTest`
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add src/main/java/com/pat/crewhive/shiftprogrammed/ShiftUser.java src/test/java/com/pat/crewhive/shiftprogrammed/ShiftUserSoftDeleteMappingTest.java
git commit -m "Applica il soft-delete a ShiftUser"
```

---

### Task 13: `User` (migra dal campo `active` standalone)

**Files:**
- Modify: `src/main/java/com/pat/crewhive/user/User.java`
- Test: `src/test/java/com/pat/crewhive/user/UserSoftDeleteMappingTest.java`

**Interfaces:**
- Consumes: `SoftDeletableEntity` (Task 1).
- Produces: usata da Task 17 (login), Task 20 (`UserService.deleteAccount`).

`User` ha già un campo `active` "semplice" aggiunto in un lavoro precedente (senza
`deletedAt`/`deletedBy`, senza `@SQLRestriction`/`@SQLDelete`): questa task lo sostituisce
con l'ereditarietà da `SoftDeletableEntity`.

- [ ] **Step 1: Scrivi il test che fallisce**

```java
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
```

- [ ] **Step 2: Esegui il test, verifica che fallisca**

Run: `./mvnw test -Dtest=UserSoftDeleteMappingTest`
Expected: FAIL

- [ ] **Step 3: Applica il mapping e rimuovi il campo `active` standalone**

In `User.java`, sostituisci l'intestazione della classe:
```java
import com.fasterxml.jackson.annotation.JsonProperty;
import com.pat.crewhive.company.Company;
import com.pat.crewhive.event.EventUsers;
import com.pat.crewhive.shiftprogrammed.ShiftUser;
import com.pat.crewhive.shiftworked.ShiftWorked;
import com.pat.crewhive.manager.UserRole;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.util.*;

@Entity
@Table(name = "users", indexes = {
        @Index(name = "idx_user_username", columnList = "username"),
        @Index(name = "idx_user_company_id", columnList = "company_id")
})
public class User {
```
con:
```java
import com.fasterxml.jackson.annotation.JsonProperty;
import com.pat.crewhive.common.audit.SoftDeletableEntity;
import com.pat.crewhive.company.Company;
import com.pat.crewhive.event.EventUsers;
import com.pat.crewhive.shiftprogrammed.ShiftUser;
import com.pat.crewhive.shiftworked.ShiftWorked;
import com.pat.crewhive.manager.UserRole;
import jakarta.persistence.*;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

import java.math.BigDecimal;
import java.util.*;

@Entity
@Table(name = "users", indexes = {
        @Index(name = "idx_user_username", columnList = "username"),
        @Index(name = "idx_user_company_id", columnList = "company_id"),
        @Index(name = "idx_user_active", columnList = "active"),
        @Index(name = "idx_user_deleted_at", columnList = "deleted_at"),
        @Index(name = "idx_user_deleted_by", columnList = "deleted_by")
})
@SQLRestriction("active = true")
@SQLDelete(sql = "UPDATE users SET active = false, deleted_at = now() WHERE user_id = ?")
public class User extends SoftDeletableEntity {
```

Poi rimuovi il campo standalone e i suoi accessor, non più necessari (ereditati da
`SoftDeletableEntity`):
```java
    /**
     * Whether the account is active. Set to false by {@code UserService.deleteAccount}
     * (soft-delete) instead of physically removing the row, so that historical records
     * (e.g. {@link com.pat.crewhive.shiftworked.ShiftWorked}) keep their reference to the user.
     */
    @Column(name = "active", nullable = false)
    private boolean active = true;

```
(rimuovi anche il blocco getter/setter corrispondente)
```java
    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

```
(nessuna sostituzione: entrambi i blocchi vanno eliminati per intero; `isActive()` resta
disponibile per chiamanti esterni perché ereditato da `SoftDeletableEntity`. Non esiste
più un `setActive(boolean)` pubblico: chi deve marcare l'utente cancellato usa
`markDeleted(User actor)`, vedi Task 20).

- [ ] **Step 4: Esegui il test, verifica che passi**

Run: `./mvnw test -Dtest=UserSoftDeleteMappingTest`
Expected: PASS

- [ ] **Step 5: Compila tutto il modulo per intercettare subito i call site rotti**

Run: `./mvnw compile`
Expected: FAIL — `AuthService.login()` (`!user.isActive()` è ancora corretto, resta
invariato) e `UserService.deleteAccount()` (`user.setActive(false)`) non compilano più
perché `setActive` non esiste. Questo è atteso: verranno sistemati rispettivamente in
Task 17 e Task 20. Annota qui che il modulo resta a compilazione rotta fino al
completamento di quelle due task (non è un errore da correggere in questa task).

- [ ] **Step 6: Commit**

```bash
git add src/main/java/com/pat/crewhive/user/User.java src/test/java/com/pat/crewhive/user/UserSoftDeleteMappingTest.java
git commit -m "Applica il soft-delete a User, rimuove il campo active standalone"
```

---

## Task 14: `EventUsersRepository` — bypass query e rimozione della bulk delete fisica

**Files:**
- Modify: `src/main/java/com/pat/crewhive/event/EventUsersRepository.java`

**Interfaces:**
- Consumes: `EventUsers` (Task 11).
- Produces: `EventUsersRepository.findByIdIncludingDeleted(UUID userId, UUID eventId): Optional<EventUsers>`, usato da Task 18.

`deleteByEventId` era usata solo da `EventService.deleteEvent`, che verrà sistemato in
Task 24 per usare `SoftDeleteSupport.softDelete` (la cascata su `Event.users` rende
questa bulk-delete ridondante): va quindi rimossa, non convertita.

Non essendoci logica nuova da collaudare con un test dedicato di unità (è una query
nativa, verificabile solo con un database reale — fuori scope, vedi Global Constraints),
questa task non segue lo schema TDD: applica la modifica e verifica la compilazione.

- [ ] **Step 1: Modifica il repository**

Sostituisci il contenuto di `EventUsersRepository.java`:
```java
package com.pat.crewhive.event;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface EventUsersRepository extends JpaRepository<EventUsers, EventUsersId> {

    @Query("""
       select e
       from EventUsers eu
       join eu.event e
       where eu.user.userId = :userId
       order by e.start asc
       """)
    List<Event> findEventsByUserId(@Param("userId") UUID userId);


    /**
     * Cerca una riga EventUsers per la sua chiave composita bypassando il filtro
     * {@code active = true} di {@code @SQLRestriction}, così un legame soft-deleted
     * in passato può essere trovato e riattivato invece di collidere sulla sua chiave
     * primaria quando lo stesso utente viene aggiunto di nuovo allo stesso evento.
     */
    @Query(value = "SELECT * FROM event_users WHERE user_id = :userId AND event_id = :eventId", nativeQuery = true)
    Optional<EventUsers> findByIdIncludingDeleted(@Param("userId") UUID userId, @Param("eventId") UUID eventId);
}
```
(l'import `java.time.LocalDate` era già inutilizzato nel file originale: lascialo
com'era se presente, non è oggetto di questa task — nello snippet sopra è già stato
rimosso perché il file viene riscritto per intero, ma se il tuo editor segnala un
diff diverso dall'originale su quella riga, non è un problema).

- [ ] **Step 2: Compila**

Run: `./mvnw compile -pl . -am -Dmaven.test.skip=true` (o semplicemente `./mvnw compile`)
Expected: nessun nuovo errore introdotto da questo file (l'unico chiamante di
`deleteByEventId` verrà sistemato in Task 24; fino ad allora `EventService` non compila
per questo motivo, atteso).

- [ ] **Step 3: Commit**

```bash
git add src/main/java/com/pat/crewhive/event/EventUsersRepository.java
git commit -m "EventUsersRepository: aggiunge findByIdIncludingDeleted, rimuove la bulk delete fisica"
```

---

## Task 15: `ShiftUserRepository` — bypass query, bulk soft-update, rimozione bulk delete

**Files:**
- Modify: `src/main/java/com/pat/crewhive/shiftprogrammed/ShiftUserRepository.java`
- Modify: `src/main/java/com/pat/crewhive/user/UserService.java` (chiamante di `deleteByUserId`)
- Test: `src/test/java/com/pat/crewhive/user/UserServiceTest.java` (aggiorna l'eventuale aspettativa sulla firma)

**Interfaces:**
- Consumes: `ShiftUser` (Task 12).
- Produces: `ShiftUserRepository.findByIdIncludingDeleted(UUID shiftId, UUID userId): Optional<ShiftUser>` (Task 19); `deleteByUserId(UUID userId, OffsetDateTime now): int` (nuova firma).

`deleteByShiftId` era usata solo da `ShiftProgrammedService.deleteShift`, sistemata in
Task 25 via `SoftDeleteSupport.softDelete` (cascata su `ShiftProgrammed.users` la rende
ridondante): va rimossa. `deleteByUserId` resta invece necessaria — è usata da
`UserService.leaveCompany` per staccare tutti i turni programmati di un utente che
lascia l'azienda, un caso che non passa da nessuna cancellazione di primo livello — e va
convertita da bulk delete a bulk soft-update.

- [ ] **Step 1: Aggiorna il test esistente per la nuova firma (fallisce a compilazione)**

`UserServiceTest.java` ha già un test che verifica questa chiamata, alla riga:
```java
        verify(shiftUserRepository).deleteByUserId(USER_ID);
```
dentro `leaveCompany_removesUserFromCompanyAndDeletesShifts()`. Sostituiscila con:
```java
        verify(shiftUserRepository).deleteByUserId(eq(USER_ID), any(java.time.OffsetDateTime.class));
```
(`eq` e `any` sono già importati staticamente nel file — vedi `import static
org.mockito.Mockito.*` — nessun nuovo import necessario).

- [ ] **Step 2: Esegui il test, verifica che fallisca**

Run: `./mvnw test -Dtest=UserServiceTest#leaveCompany_removesUserFromCompanyAndDeletesShifts`
Expected: FAIL (compilazione: `deleteByUserId(UUID)` non accetta ancora due argomenti)

- [ ] **Step 3: Riscrivi `ShiftUserRepository`**

```java
package com.pat.crewhive.shiftprogrammed;


import com.pat.crewhive.user.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ShiftUserRepository extends JpaRepository<ShiftUser, ShiftUserId> {

    @Query("""
        select distinct u
        from ShiftUser su
        join su.user u
        where su.shift.shiftProgrammedId = :shiftId
        order by u.email asc
        """)
    List<User> findUsersByShiftId(@Param("shiftId") UUID shiftId);


    /**
     * Soft-delete di tutti i legami ShiftUser di un utente (es. quando lascia
     * l'azienda), senza caricare ogni riga nel persistence context.
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update ShiftUser su set su.active = false, su.deletedAt = :now where su.user.userId = :userId and su.active = true")
    int deleteByUserId(@Param("userId") UUID userId, @Param("now") OffsetDateTime now);

    /**
     * Cerca una riga ShiftUser per la sua chiave composita bypassando il filtro
     * {@code active = true} di {@code @SQLRestriction}, così un legame soft-deleted
     * in passato può essere trovato e riattivato invece di collidere sulla sua chiave
     * primaria quando lo stesso utente viene aggiunto di nuovo allo stesso turno.
     */
    @Query(value = "SELECT * FROM shift_user WHERE shift_programmed_id = :shiftId AND user_id = :userId", nativeQuery = true)
    Optional<ShiftUser> findByIdIncludingDeleted(@Param("shiftId") UUID shiftId, @Param("userId") UUID userId);
}
```

- [ ] **Step 4: Aggiorna il chiamante in `UserService.leaveCompany`**

In `UserService.java`, sostituisci:
```java
        shiftUserRepository.deleteByUserId(userId);
```
con:
```java
        shiftUserRepository.deleteByUserId(userId, OffsetDateTime.now());
```
e aggiungi l'import in cima al file:
```java
import java.time.OffsetDateTime;
```

- [ ] **Step 5: Esegui il test, verifica che passi**

Run: `./mvnw test -Dtest=UserServiceTest#leaveCompany_removesUserFromCompanyAndDeletesShifts`
Expected: PASS

- [ ] **Step 6: Commit**

```bash
git add src/main/java/com/pat/crewhive/shiftprogrammed/ShiftUserRepository.java src/main/java/com/pat/crewhive/user/UserService.java src/test/java/com/pat/crewhive/user/UserServiceTest.java
git commit -m "ShiftUserRepository: bulk soft-update, findByIdIncludingDeleted, rimuove la bulk delete fisica"
```

---

## Task 16: `UserRepository.existsInactiveByEmail`

**Files:**
- Modify: `src/main/java/com/pat/crewhive/user/UserRepository.java`

**Interfaces:**
- Consumes: nulla.
- Produces: `UserRepository.existsInactiveByEmail(String email): boolean`, usato da Task 17.

Query nativa (bypassa `@SQLRestriction` di proposito): non testabile senza un database
reale, coerente con Global Constraints. Nessun passo TDD: applica e compila.

- [ ] **Step 1: Aggiungi il metodo**

In `UserRepository.java`, aggiungi dopo `existsByEmail`:
```java
    boolean existsByEmail(String email);

    /**
     * Verifica se esiste un utente disattivato con questa email, bypassando il
     * filtro {@code active = true} di {@code @SQLRestriction}. Usato dal login per
     * distinguere "account disattivato" da "email mai registrata".
     */
    @Query(value = "SELECT EXISTS(SELECT 1 FROM users WHERE email = :email AND active = false)", nativeQuery = true)
    boolean existsInactiveByEmail(@Param("email") String email);
```

- [ ] **Step 2: Compila**

Run: `./mvnw compile`
Expected: PASS (nessun chiamante ancora, il metodo è solo aggiunto)

- [ ] **Step 3: Commit**

```bash
git add src/main/java/com/pat/crewhive/user/UserRepository.java
git commit -m "UserRepository: aggiunge existsInactiveByEmail"
```

---

## Task 17: `AuthService.login()` — messaggio distinto per account disattivato

**Files:**
- Modify: `src/main/java/com/pat/crewhive/authuser/AuthService.java`
- Modify: `src/test/java/com/pat/crewhive/authuser/AuthServiceTest.java`

**Interfaces:**
- Consumes: `UserRepository.existsInactiveByEmail` (Task 16), `User` con `@SQLRestriction` (Task 13).
- Produces: nessuna nuova interfaccia; risolve la compilazione rotta di Task 13 per
  quanto riguarda `AuthService` (il controllo `!user.isActive()` era già corretto e resta
  tale — nulla da fare lì — ma va cambiato il modo in cui `user` viene caricato).

Con `@SQLRestriction("active = true")` su `User` (Task 13), `userRepository.findByEmail`
non trova più gli account disattivati: `userService.getUserByEmail` (che quella query la
usa) lancerebbe `ResourceNotFoundException` invece di permettere il controllo esplicito.
Va quindi sostituita con `userRepository.findByEmail` diretto (già iniettato in
`AuthService`) più la query di esistenza di Task 16. `userService` diventa a questo punto
un campo inutilizzato in `AuthService` (era usato solo qui): va rimosso insieme al suo
parametro nel costruttore.

- [ ] **Step 1: Aggiorna i test di `login()` esistenti (falliranno finché non tocchi il codice)**

In `AuthServiceTest.java`:

1. Rimuovi il campo mock e il parametro dal costruttore:
```java
    @Mock
    private UserService userService;
```
(rimuovi l'intero blocco) e nel `@BeforeEach`:
```java
        authService = new AuthService(
                userService, jwtService, refreshTokenService, userRepository,
                roleService, passwordUtil, emailUtil, stringUtils, tokenBlackListService
        );
```
diventa:
```java
        authService = new AuthService(
                jwtService, refreshTokenService, userRepository,
                roleService, passwordUtil, emailUtil, stringUtils, tokenBlackListService
        );
```

2. In ognuno dei quattro test `login_...` esistenti, sostituisci
`when(userService.getUserByEmail(...))` con `when(userRepository.findByEmail(...))`
avvolto in `Optional.of(...)`. Ad esempio in `login_returnsTokens_whenCredentialsAreValid`:
```java
        when(userService.getUserByEmail("mario.rossi@example.com")).thenReturn(user);
```
diventa:
```java
        when(userRepository.findByEmail("mario.rossi@example.com")).thenReturn(Optional.of(user));
```
(stessa sostituzione, con `anyString()` al posto della email letterale dov'è già così
nel test originale, per `login_doesNotInvalidateAnyToken_whenUserHasNoExistingSession`,
`login_passesCompanyId_whenUserBelongsToACompany` e
`login_throwsBadCredentialsException_whenPasswordDoesNotMatch`).

3. Aggiungi l'import:
```java
import java.util.Optional;
```

4. Aggiungi due nuovi test dopo `login_throwsBadCredentialsException_whenPasswordDoesNotMatch`:
```java
    @Test
    void login_throwsAccountDisabled_whenUserIsDeactivated() {
        AuthRequestDTO request = new AuthRequestDTO("mario.rossi@example.com", "P@ssw0rd!");

        when(stringUtils.normalizeString(anyString())).thenReturn("mario.rossi@example.com");
        when(userRepository.findByEmail("mario.rossi@example.com")).thenReturn(Optional.empty());
        when(userRepository.existsInactiveByEmail("mario.rossi@example.com")).thenReturn(true);

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(BadCredentialsException.class)
                .hasMessage("Account disabled");

        verifyNoInteractions(refreshTokenService, jwtService);
    }

    @Test
    void login_throwsUserNotFound_whenEmailDoesNotExistAtAll() {
        AuthRequestDTO request = new AuthRequestDTO("nobody@example.com", "P@ssw0rd!");

        when(stringUtils.normalizeString(anyString())).thenReturn("nobody@example.com");
        when(userRepository.findByEmail("nobody@example.com")).thenReturn(Optional.empty());
        when(userRepository.existsInactiveByEmail("nobody@example.com")).thenReturn(false);

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(ResourceNotFoundException.class);

        verifyNoInteractions(refreshTokenService, jwtService);
    }
```
Aggiungi l'import:
```java
import com.pat.crewhive.security.exception.custom.ResourceNotFoundException;
```

- [ ] **Step 2: Esegui i test, verifica che falliscano**

Run: `./mvnw test -Dtest=AuthServiceTest`
Expected: FAIL (compilazione: `AuthService` ha ancora il costruttore a 9 argomenti con
`UserService`, e non ha ancora `existsInactiveByEmail` nel suo flusso)

- [ ] **Step 3: Riscrivi `AuthService.login()` e rimuovi `userService`**

Rimuovi dai campi e dal costruttore:
```java
    private final UserService userService;
```
e dal costruttore:
```java
    public AuthService(UserService userService,
                       JwtService jwtService,
                       RefreshTokenService refreshTokenService,
                       UserRepository userRepository,
                       RoleService roleService,
                       PasswordUtil passwordUtil,
                       EmailUtil emailUtil,
                       StringUtils stringUtils,
                       TokenBlackListService tokenBlackListService) {
        this.userService = userService;
        this.jwtService = jwtService;
        this.refreshTokenService = refreshTokenService;
        this.userRepository = userRepository;
        this.roleService = roleService;
        this.passwordUtil = passwordUtil;
        this.emailUtil = emailUtil;
        this.stringUtils = stringUtils;
        this.tokenBlackListService = tokenBlackListService;
    }
```
diventa:
```java
    public AuthService(JwtService jwtService,
                       RefreshTokenService refreshTokenService,
                       UserRepository userRepository,
                       RoleService roleService,
                       PasswordUtil passwordUtil,
                       EmailUtil emailUtil,
                       StringUtils stringUtils,
                       TokenBlackListService tokenBlackListService) {
        this.jwtService = jwtService;
        this.refreshTokenService = refreshTokenService;
        this.userRepository = userRepository;
        this.roleService = roleService;
        this.passwordUtil = passwordUtil;
        this.emailUtil = emailUtil;
        this.stringUtils = stringUtils;
        this.tokenBlackListService = tokenBlackListService;
    }
```
Rimuovi l'import:
```java
import com.pat.crewhive.user.UserService;
```
Aggiungi:
```java
import com.pat.crewhive.security.exception.custom.ResourceNotFoundException;
```

Nel corpo di `login`, sostituisci:
```java
        String normalizedEmail = stringUtils.normalizeString(request.email());
        User user = userService.getUserByEmail(normalizedEmail);

        if (passwordUtil.NotMatches(request.password(), user.getPassword())) {
            log.error("Invalid password for user: {}", normalizedEmail);

            throw new BadCredentialsException("Invalid credentials");
        }

        if (!user.isActive()) {
            log.error("Login attempt for deactivated account: {}", normalizedEmail);

            throw new BadCredentialsException("Account disabled");
        }

        log.info("User {} authenticated successfully", normalizedEmail);
```
con:
```java
        String normalizedEmail = stringUtils.normalizeString(request.email());

        // @SQLRestriction su User nasconde gli account disattivati a findByEmail: se
        // la riga non c'è, verifichiamo separatamente se esiste ma è inattiva, per dare
        // un messaggio distinto. Nota: questo significa che per un account disattivato
        // la password non viene nemmeno controllata - è una conseguenza accettata del
        // filtro automatico, non una scelta di questo metodo.
        User user = userRepository.findByEmail(normalizedEmail)
                .orElseGet(() -> {
                    if (userRepository.existsInactiveByEmail(normalizedEmail)) {
                        log.error("Login attempt for deactivated account: {}", normalizedEmail);
                        throw new BadCredentialsException("Account disabled");
                    }
                    throw new ResourceNotFoundException("User not found");
                });

        if (passwordUtil.NotMatches(request.password(), user.getPassword())) {
            log.error("Invalid password for user: {}", normalizedEmail);

            throw new BadCredentialsException("Invalid credentials");
        }

        log.info("User {} authenticated successfully", normalizedEmail);
```

- [ ] **Step 4: Esegui i test, verifica che passino**

Run: `./mvnw test -Dtest=AuthServiceTest`
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add src/main/java/com/pat/crewhive/authuser/AuthService.java src/test/java/com/pat/crewhive/authuser/AuthServiceTest.java
git commit -m "AuthService.login: messaggio Account disabled distinto, rimuove UserService inutilizzato"
```

---

## Task 18: `Event.addUser`/`EventService.patchEvent` — riattivazione invece di duplicato

**Files:**
- Modify: `src/main/java/com/pat/crewhive/event/Event.java`
- Modify: `src/main/java/com/pat/crewhive/event/EventService.java`
- Test: `src/test/java/com/pat/crewhive/event/EventServiceTest.java`

**Interfaces:**
- Consumes: `EventUsersRepository.findByIdIncludingDeleted` (Task 14), `EventUsers.restore()` (da `SoftDeletableEntity`, Task 1).
- Produces: `Event.attachUser(EventUsers link): void`.

`Event.addUser(User u)` resta invariato e continua a essere usato in `createEvent` (evento
nuovo: nessuna collisione di PK possibile). Solo il ramo "aggiungi utenti a un evento
esistente" di `patchEvent` deve gestire la riattivazione.

- [ ] **Step 1: Scrivi il test che fallisce**

Aggiungi in `EventServiceTest.java`:
```java
    @Test
    void patchEvent_reactivatesPreviouslySoftDeletedLink_insteadOfCreatingDuplicate() {
        UUID eventId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        Event event = new Event();
        ReflectionTestUtils.setField(event, "eventId", eventId);
        event.setEventName("Riunione");
        event.setStart(OffsetDateTime.parse("2026-08-21T09:00:00Z"));
        event.setEnd(OffsetDateTime.parse("2026-08-21T17:00:00Z"));
        event.setColor("#FF0000");

        User user = new User("user@example.com", "Mario", "Rossi", "encoded-pwd");
        ReflectionTestUtils.setField(user, "userId", userId);

        EventUsers softDeletedLink = new EventUsers(user, event);
        softDeletedLink.markDeleted(user);

        PatchEventDTO dto = new PatchEventDTO(
                eventId, "Riunione", null,
                OffsetDateTime.parse("2026-08-21T09:00:00Z"),
                OffsetDateTime.parse("2026-08-21T17:00:00Z"),
                "FF0000", EventType.PRIVATE, java.util.Set.of(userId)
        );

        when(eventRepository.findByIdWithParticipants(eventId)).thenReturn(java.util.Optional.of(event));
        when(eventTypeRepository.getReferenceById((short) 2)).thenReturn(new EventTypeEntity((short) 2, "Private"));
        when(userService.getUsersByIds(java.util.Set.of(userId))).thenReturn(List.of(user));
        when(eventUsersRepository.findByIdIncludingDeleted(userId, eventId)).thenReturn(java.util.Optional.of(softDeletedLink));
        when(stringUtils.normalizeString("Riunione")).thenReturn("Riunione");
        when(eventRepository.save(event)).thenReturn(event);

        eventService.patchEvent(dto);

        assertThat(softDeletedLink.isActive()).isTrue();
        assertThat(softDeletedLink.getDeletedAt()).isNull();
        assertThat(softDeletedLink.getDeletedBy()).isNull();
        assertThat(event.getUsers()).containsExactly(softDeletedLink);
    }
```
Verifica che `java.time.OffsetDateTime` sia già importato nel file (lo è, usato da
`buildEventWithParticipant`); gli altri riferimenti `java.util.Optional`/`java.util.Set`
sono scritti per esteso nello snippet per non richiedere nuovi import.

- [ ] **Step 2: Esegui il test, verifica che fallisca**

Run: `./mvnw test -Dtest=EventServiceTest#patchEvent_reactivatesPreviouslySoftDeletedLink_insteadOfCreatingDuplicate`
Expected: FAIL (compilazione: `findByIdIncludingDeleted` non esiste ancora su
`EventUsersRepository`... in realtà esiste già da Task 14; fallirà invece perché
`patchEvent` chiama ancora `event::addUser` che ricostruisce sempre un `EventUsers`
nuovo, quindi l'asserzione su `containsExactly(softDeletedLink)` fallisce — la
collezione conterrebbe un'istanza diversa)

- [ ] **Step 3: Aggiungi `Event.attachUser` e aggiorna `EventService.patchEvent`**

In `Event.java`, aggiungi dopo il metodo `addUser`:
```java
    /**
     * Collega un EventUsers già risolto (nuovo, oppure una riga soft-deleted
     * riattivata dal chiamante) a entrambi i lati dell'associazione. A differenza di
     * {@link #addUser}, non verifica duplicati né costruisce il link: quella
     * decisione richiede una query sul repository (per trovare una riga soft-deleted
     * che occupa già la stessa chiave primaria), possibile solo nel service layer.
     */
    public void attachUser(EventUsers link) {
        this.users.add(link);
        link.getUser().getPersonalEvents().add(link);
    }
```

In `EventService.java`, sostituisci nel metodo `patchEvent`:
```java
            // aggiungi i nuovi mancanti
            Set<UUID> toAdd = new HashSet<>(newUserIds);
            toAdd.removeAll(existingIds);
            if (!toAdd.isEmpty()) {
                List<User> usersToAdd = userService.getUsersByIds(toAdd);
                usersToAdd.forEach(event::addUser);
            }
```
con:
```java
            // aggiungi i nuovi mancanti (riattivando un legame soft-deleted se esiste
            // già per questa coppia utente/evento, per non collidere sulla sua PK)
            Set<UUID> toAdd = new HashSet<>(newUserIds);
            toAdd.removeAll(existingIds);
            if (!toAdd.isEmpty()) {
                List<User> usersToAdd = userService.getUsersByIds(toAdd);
                for (User user : usersToAdd) {
                    EventUsers link = eventUsersRepository
                            .findByIdIncludingDeleted(user.getUserId(), event.getEventId())
                            .map(existing -> { existing.restore(); return existing; })
                            .orElseGet(() -> new EventUsers(user, event));
                    event.attachUser(link);
                }
            }
```

- [ ] **Step 4: Esegui il test, verifica che passi**

Run: `./mvnw test -Dtest=EventServiceTest#patchEvent_reactivatesPreviouslySoftDeletedLink_insteadOfCreatingDuplicate`
Expected: PASS

- [ ] **Step 5: Esegui tutta la classe di test per non aver rotto nulla**

Run: `./mvnw test -Dtest=EventServiceTest`
Expected: PASS

- [ ] **Step 6: Commit**

```bash
git add src/main/java/com/pat/crewhive/event/Event.java src/main/java/com/pat/crewhive/event/EventService.java src/test/java/com/pat/crewhive/event/EventServiceTest.java
git commit -m "Event/EventService: riattiva un legame EventUsers soft-deleted invece di duplicarlo"
```

---

## Task 19: `ShiftProgrammed.addUser`/`ShiftProgrammedService.patchShift` — riattivazione

**Files:**
- Modify: `src/main/java/com/pat/crewhive/shiftprogrammed/ShiftProgrammed.java`
- Modify: `src/main/java/com/pat/crewhive/shiftprogrammed/ShiftProgrammedService.java`
- Test: `src/test/java/com/pat/crewhive/shiftprogrammed/ShiftProgrammedServiceTest.java`

**Interfaces:**
- Consumes: `ShiftUserRepository.findByIdIncludingDeleted` (Task 15), `ShiftUser.restore()` (da `SoftDeletableEntity`, Task 1).
- Produces: `ShiftProgrammed.attachUser(ShiftUser link): void`.

Stesso schema di Task 18, applicato a `ShiftProgrammed`/`ShiftUser`.
`ShiftProgrammed.addUser(User u)` resta invariato per `createShift` (turno nuovo, nessuna
collisione possibile).

- [ ] **Step 1: Scrivi il test che fallisce**

Aggiungi in `ShiftProgrammedServiceTest.java`:
```java
    @Test
    void patchShift_reactivatesPreviouslySoftDeletedLink_insteadOfCreatingDuplicate() {
        UUID shiftId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        ShiftProgrammed shift = new ShiftProgrammed();
        ReflectionTestUtils.setField(shift, "shiftProgrammedId", shiftId);
        shift.setShiftName("Turno mattina");
        shift.setStart(OffsetDateTime.parse("2026-08-21T09:00:00Z"));
        shift.setEnd(OffsetDateTime.parse("2026-08-21T17:00:00Z"));
        shift.setColor("#00FF00");

        User user = buildUser(userId, "Mario", "Rossi");

        ShiftUser softDeletedLink = new ShiftUser(shift, user);
        softDeletedLink.markDeleted(user);

        PatchShiftProgrammedDTO dto = new PatchShiftProgrammedDTO(
                shiftId, "Turno mattina", null,
                OffsetDateTime.parse("2026-08-21T09:00:00Z"),
                OffsetDateTime.parse("2026-08-21T17:00:00Z"),
                "00FF00", java.util.Set.of(userId)
        );

        when(shiftProgrammedRepository.findByIdWithWorkers(shiftId)).thenReturn(java.util.Optional.of(shift));
        when(stringUtils.normalizeString("Turno mattina")).thenReturn("Turno mattina");
        when(userService.getUsersByIds(java.util.Set.of(userId))).thenReturn(List.of(user));
        when(shiftUserRepository.findByIdIncludingDeleted(shiftId, userId)).thenReturn(java.util.Optional.of(softDeletedLink));

        shiftProgrammedService.patchShift(UUID.randomUUID(), dto);

        assertThat(softDeletedLink.isActive()).isTrue();
        assertThat(softDeletedLink.getDeletedAt()).isNull();
        assertThat(softDeletedLink.getDeletedBy()).isNull();
        assertThat(shift.getUsers()).containsExactly(softDeletedLink);
    }
```

- [ ] **Step 2: Esegui il test, verifica che fallisca**

Run: `./mvnw test -Dtest=ShiftProgrammedServiceTest#patchShift_reactivatesPreviouslySoftDeletedLink_insteadOfCreatingDuplicate`
Expected: FAIL (l'asserzione su `containsExactly(softDeletedLink)` fallisce: `patchShift`
costruisce ancora un `ShiftUser` nuovo tramite `shift::addUser`)

- [ ] **Step 3: Aggiungi `ShiftProgrammed.attachUser` e aggiorna `ShiftProgrammedService.patchShift`**

In `ShiftProgrammed.java`, aggiungi dopo il metodo `addUser`:
```java
    /**
     * Collega uno ShiftUser già risolto (nuovo, oppure una riga soft-deleted
     * riattivata dal chiamante) a entrambi i lati dell'associazione. A differenza di
     * {@link #addUser}, non verifica duplicati né costruisce il link: quella
     * decisione richiede una query sul repository (per trovare una riga soft-deleted
     * che occupa già la stessa chiave primaria), possibile solo nel service layer.
     */
    public void attachUser(ShiftUser link) {
        this.users.add(link);
        link.getUser().getShiftUsers().add(link);
    }
```

In `ShiftProgrammedService.java`, sostituisci nel metodo `patchShift`:
```java
            // Aggiungi i nuovi
            if (!newIds.isEmpty()) {
                Set<UUID> toAdd = new HashSet<>(newIds);
                toAdd.removeAll(current);
                if (!toAdd.isEmpty()) {
                    userService.getUsersByIds(toAdd).forEach(shift::addUser);
                }
            } else {
```
con:
```java
            // Aggiungi i nuovi (riattivando un legame soft-deleted se esiste già per
            // questa coppia utente/turno, per non collidere sulla sua PK)
            if (!newIds.isEmpty()) {
                Set<UUID> toAdd = new HashSet<>(newIds);
                toAdd.removeAll(current);
                if (!toAdd.isEmpty()) {
                    for (User user : userService.getUsersByIds(toAdd)) {
                        ShiftUser link = shiftUserRepository
                                .findByIdIncludingDeleted(shift.getShiftProgrammedId(), user.getUserId())
                                .map(existing -> { existing.restore(); return existing; })
                                .orElseGet(() -> new ShiftUser(shift, user));
                        shift.attachUser(link);
                    }
                }
            } else {
```

- [ ] **Step 4: Esegui il test, verifica che passi**

Run: `./mvnw test -Dtest=ShiftProgrammedServiceTest#patchShift_reactivatesPreviouslySoftDeletedLink_insteadOfCreatingDuplicate`
Expected: PASS

- [ ] **Step 5: Esegui tutta la classe di test**

Run: `./mvnw test -Dtest=ShiftProgrammedServiceTest`
Expected: PASS

- [ ] **Step 6: Commit**

```bash
git add src/main/java/com/pat/crewhive/shiftprogrammed/ShiftProgrammed.java src/main/java/com/pat/crewhive/shiftprogrammed/ShiftProgrammedService.java src/test/java/com/pat/crewhive/shiftprogrammed/ShiftProgrammedServiceTest.java
git commit -m "ShiftProgrammed/ShiftProgrammedService: riattiva un legame ShiftUser soft-deleted invece di duplicarlo"
```

---

## Task 20: `UserService.deleteAccount` → `SoftDeleteSupport`

**Files:**
- Modify: `src/main/java/com/pat/crewhive/user/UserService.java`
- Test: `src/test/java/com/pat/crewhive/user/UserServiceTest.java`

**Interfaces:**
- Consumes: `SoftDeleteSupport.softDelete` (Task 2), `User` (Task 13, risolve la
  compilazione rotta da Task 13 per `deleteAccount`).

- [ ] **Step 1: Scrivi il test che fallisce**

Aggiungi in `UserServiceTest.java` (riusa l'helper `buildUser(UUID, Company)` già
presente nel file, con `company = null` visto che non serve per questo test):
```java
    @Test
    void deleteAccount_softDeletesTheUserWithItselfAsActor() {
        User user = buildUser(USER_ID, null);

        when(userRepository.findById(USER_ID)).thenReturn(java.util.Optional.of(user));

        userService.deleteAccount(USER_ID);

        assertThat(user.isActive()).isFalse();
        assertThat(user.getDeletedBy()).isSameAs(user);
        assertThat(user.getDeletedAt()).isNotNull();
        verify(refreshTokenService).deleteTokenByUser(user);

        org.mockito.InOrder order = org.mockito.Mockito.inOrder(userRepository);
        order.verify(userRepository).save(user);
        order.verify(userRepository).delete(user);
    }
```

- [ ] **Step 2: Esegui il test, verifica che fallisca**

Run: `./mvnw test -Dtest=UserServiceTest#deleteAccount_softDeletesTheUserWithItselfAsActor`
Expected: FAIL (compilazione: `user.setActive(false)` non esiste più da Task 13; il
modulo era già a compilazione rotta su questo punto)

- [ ] **Step 3: Riscrivi `deleteAccount`**

In `UserService.java`, sostituisci:
```java
    @Transactional
    public void deleteAccount(UUID userId) {

        User user = getUserById(userId);

        refreshTokenService.deleteTokenByUser(user);

        user.setActive(false);
        userRepository.save(user);

        log.info("Deactivated account for user: {}", userId);
    }
```
con:
```java
    @Transactional
    public void deleteAccount(UUID userId) {

        User user = getUserById(userId);

        refreshTokenService.deleteTokenByUser(user);

        SoftDeleteSupport.softDelete(userRepository, user, user);

        log.info("Deactivated account for user: {}", userId);
    }
```
e aggiungi l'import:
```java
import com.pat.crewhive.common.audit.SoftDeleteSupport;
```

- [ ] **Step 4: Esegui il test, verifica che passi**

Run: `./mvnw test -Dtest=UserServiceTest#deleteAccount_softDeletesTheUserWithItselfAsActor`
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add src/main/java/com/pat/crewhive/user/UserService.java src/test/java/com/pat/crewhive/user/UserServiceTest.java
git commit -m "UserService.deleteAccount: usa SoftDeleteSupport"
```

---

## Task 21: `CompanyService.deleteCompany` → `SoftDeleteSupport`

**Files:**
- Modify: `src/main/java/com/pat/crewhive/company/CompanyService.java`
- Test: `src/test/java/com/pat/crewhive/company/CompanyServiceTest.java`

**Interfaces:**
- Consumes: `SoftDeleteSupport.softDelete` (Task 2), `Company` (Task 4).

- [ ] **Step 1: Scrivi il test che fallisce**

Aggiungi in `CompanyServiceTest.java` (i mock si chiamano esattamente `companyRepository`,
`userService`, `companyAccessService`; `User` e `ReflectionTestUtils` sono già importati
nel file, nessun nuovo import necessario):
```java
    @Test
    void deleteCompany_softDeletesTheCompanyWithManagerAsActor() {
        UUID companyId = UUID.randomUUID();
        UUID managerId = UUID.randomUUID();

        Company company = new Company();
        ReflectionTestUtils.setField(company, "companyId", companyId);

        User manager = new User("manager@example.com", "Manager", "Rossi", "encoded-pwd");
        ReflectionTestUtils.setField(manager, "userId", managerId);

        when(companyAccessService.getCompanyById(companyId)).thenReturn(company);
        when(companyAccessService.isNotPartOfCompany(managerId, companyId)).thenReturn(false);
        when(userService.getUserById(managerId)).thenReturn(manager);

        companyService.deleteCompany(companyId, managerId);

        assertThat(company.isActive()).isFalse();
        assertThat(company.getDeletedBy()).isSameAs(manager);
        verify(companyAccessService).removeCompanyFromUsers(companyId);

        org.mockito.InOrder order = org.mockito.Mockito.inOrder(companyRepository);
        order.verify(companyRepository).save(company);
        order.verify(companyRepository).delete(company);
    }
```

- [ ] **Step 2: Esegui il test, verifica che fallisca**

Run: `./mvnw test -Dtest=CompanyServiceTest#deleteCompany_softDeletesTheCompanyWithManagerAsActor`
Expected: FAIL (`deleteCompany` chiama ancora `companyRepository.deleteById`, non `save`+`delete` sull'entità)

- [ ] **Step 3: Riscrivi `deleteCompany`**

In `CompanyService.java`, sostituisci:
```java
    public void deleteCompany(UUID companyId, UUID managerId) {

        if(companyAccessService.isNotPartOfCompany(managerId, companyAccessService.getCompanyById(companyId).getCompanyId())) {

            log.error("deleteCompany: Manager {} may be not part of company {}", managerId, companyId);
            throw new AuthorizationDeniedException("Manager does not belong to the specified company.");
        }

        companyAccessService.removeCompanyFromUsers(companyId);

        companyRepository.deleteById(companyId);

        log.info("Company with ID {} deleted successfully", companyId);
    }
```
con:
```java
    public void deleteCompany(UUID companyId, UUID managerId) {

        Company company = companyAccessService.getCompanyById(companyId);

        if(companyAccessService.isNotPartOfCompany(managerId, company.getCompanyId())) {

            log.error("deleteCompany: Manager {} may be not part of company {}", managerId, companyId);
            throw new AuthorizationDeniedException("Manager does not belong to the specified company.");
        }

        companyAccessService.removeCompanyFromUsers(companyId);

        User manager = userService.getUserById(managerId);
        SoftDeleteSupport.softDelete(companyRepository, company, manager);

        log.info("Company with ID {} deleted successfully", companyId);
    }
```
e aggiungi l'import:
```java
import com.pat.crewhive.common.audit.SoftDeleteSupport;
```

- [ ] **Step 4: Esegui il test, verifica che passi**

Run: `./mvnw test -Dtest=CompanyServiceTest#deleteCompany_softDeletesTheCompanyWithManagerAsActor`
Expected: PASS

- [ ] **Step 5: Esegui tutta la classe di test**

Run: `./mvnw test -Dtest=CompanyServiceTest`
Expected: PASS

- [ ] **Step 6: Commit**

```bash
git add src/main/java/com/pat/crewhive/company/CompanyService.java src/test/java/com/pat/crewhive/company/CompanyServiceTest.java
git commit -m "CompanyService.deleteCompany: usa SoftDeleteSupport"
```

---

## Task 22: `RoleService.deleteRole` → `SoftDeleteSupport` + attore dal manager

**Files:**
- Modify: `src/main/java/com/pat/crewhive/manager/RoleService.java`
- Modify: `src/main/java/com/pat/crewhive/manager/ManagerController.java`

**Interfaces:**
- Consumes: `SoftDeleteSupport.softDelete` (Task 2), `Role` (Task 7).
- Produces: `RoleService.deleteRole(String roleName, UUID companyId, UUID actorId)` (firma cambiata, un argomento in più).

Non esiste alcun test per `deleteRole` in questo repository (né `RoleServiceTest` né
`ManagerControllerTest` esistono, verificato: `grep -rn "deleteRole" src/test/java`
non trova nulla), quindi questa task non segue lo schema TDD rosso/verde: applica le
modifiche e verifica solo che il modulo compili e il resto della suite `manager` resti
verde.

- [ ] **Step 1: Modifica `RoleService.deleteRole`**

In `RoleService.java`, sostituisci:
```java
    @Transactional
    public void deleteRole(String roleName, UUID companyId) {

        String normalizedRole = stringUtils.normalizeRole(roleName);

        Company company = companyService.getCompanyById(companyId);

        Role role = roleRepository.findByRoleNameIgnoreCaseAndCompany(normalizedRole, company)
                .orElseThrow(() -> new ResourceNotFoundException("Role not found"));

        if (role.getUsers() != null && !role.getUsers().isEmpty()) {

            log.error("Cannot delete role {} because it is assigned to users", roleName);
            throw new IllegalStateException("Cannot delete role because it is assigned to users");
        }

        roleRepository.delete(role);
        log.info("Role {} deleted successfully", roleName);
    }
```
con:
```java
    @Transactional
    public void deleteRole(String roleName, UUID companyId, UUID actorId) {

        String normalizedRole = stringUtils.normalizeRole(roleName);

        Company company = companyService.getCompanyById(companyId);

        Role role = roleRepository.findByRoleNameIgnoreCaseAndCompany(normalizedRole, company)
                .orElseThrow(() -> new ResourceNotFoundException("Role not found"));

        if (role.getUsers() != null && !role.getUsers().isEmpty()) {

            log.error("Cannot delete role {} because it is assigned to users", roleName);
            throw new IllegalStateException("Cannot delete role because it is assigned to users");
        }

        User actor = userService.getUserById(actorId);
        SoftDeleteSupport.softDelete(roleRepository, role, actor);

        log.info("Role {} deleted successfully", roleName);
    }
```
e aggiungi l'import:
```java
import com.pat.crewhive.common.audit.SoftDeleteSupport;
```
(`User` è già importato in `RoleService.java`, usato da `assignRole`).

- [ ] **Step 2: Thread l'attore dal controller**

In `ManagerController.java`, sostituisci:
```java
        roleService.deleteRole(roleName, companyId);
```
con:
```java
        roleService.deleteRole(roleName, companyId, cud.getUserId());
```

- [ ] **Step 3: Compila ed esegui la suite manager**

Run: `./mvnw test -Dtest=com.pat.crewhive.manager.**`
Expected: PASS (nessun test esistente rotto: verificato che non ce ne fossero per `deleteRole`)

- [ ] **Step 4: Commit**

```bash
git add src/main/java/com/pat/crewhive/manager/RoleService.java src/main/java/com/pat/crewhive/manager/ManagerController.java
git commit -m "RoleService.deleteRole: usa SoftDeleteSupport, riceve l'attore dal controller"
```

---

## Task 23: `ShiftTemplateService.deleteShiftTemplate` → `SoftDeleteSupport` + attore

**Files:**
- Modify: `src/main/java/com/pat/crewhive/shifttemplate/ShiftTemplateService.java`
- Modify: `src/main/java/com/pat/crewhive/shifttemplate/ShiftTemplateController.java`
- Modify: `src/main/java/com/pat/crewhive/shifttemplate/ShiftTemplateControllerInterface.java`

**Interfaces:**
- Consumes: `SoftDeleteSupport.softDelete` (Task 2), `ShiftTemplate` (Task 5).
- Produces: `ShiftTemplateService.deleteShiftTemplate(String shiftName, UUID companyId, UUID actorId)` (firma cambiata); `ShiftTemplateService` guadagna una dipendenza da `UserService`.

Questo endpoint oggi non riceve affatto il principal autenticato: va aggiunto seguendo lo
stesso pattern già usato in `CompanyController`/`EventController`
(`@AuthenticationPrincipal CustomUserDetails cud`).

- [ ] **Step 1: Aggiungi la dipendenza `UserService` a `ShiftTemplateService`**

In `ShiftTemplateService.java`, sostituisci:
```java
import com.pat.crewhive.company.Company;
import com.pat.crewhive.company.CompanyService;
import com.pat.crewhive.security.exception.custom.ResourceAlreadyExistsException;
import com.pat.crewhive.security.exception.custom.ResourceNotFoundException;
import com.pat.crewhive.common.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class ShiftTemplateService {

    private static final Logger log = LoggerFactory.getLogger(ShiftTemplateService.class);

    private final ShiftTemplateRepository repo;
    private final CompanyService companyService;
    private final StringUtils stringUtils;

    public ShiftTemplateService(ShiftTemplateRepository repo,
                                CompanyService companyService,
                                StringUtils stringUtils) {
        this.repo = repo;
        this.companyService = companyService;
        this.stringUtils = stringUtils;
    }
```
con:
```java
import com.pat.crewhive.common.audit.SoftDeleteSupport;
import com.pat.crewhive.company.Company;
import com.pat.crewhive.company.CompanyService;
import com.pat.crewhive.security.exception.custom.ResourceAlreadyExistsException;
import com.pat.crewhive.security.exception.custom.ResourceNotFoundException;
import com.pat.crewhive.common.StringUtils;
import com.pat.crewhive.user.User;
import com.pat.crewhive.user.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class ShiftTemplateService {

    private static final Logger log = LoggerFactory.getLogger(ShiftTemplateService.class);

    private final ShiftTemplateRepository repo;
    private final CompanyService companyService;
    private final StringUtils stringUtils;
    private final UserService userService;

    public ShiftTemplateService(ShiftTemplateRepository repo,
                                CompanyService companyService,
                                StringUtils stringUtils,
                                UserService userService) {
        this.repo = repo;
        this.companyService = companyService;
        this.stringUtils = stringUtils;
        this.userService = userService;
    }
```

- [ ] **Step 2: Riscrivi `deleteShiftTemplate`**

Sostituisci:
```java
    @Transactional
    public void deleteShiftTemplate(String shiftName, UUID companyId) {

        log.info("Deleting Shift Template '{}' for company {}", shiftName, companyId);

        String normalizedShiftName = stringUtils.normalizeString(shiftName);

        ShiftTemplate shiftTemplate = repo.findByShiftNameAndCompanyCompanyId(normalizedShiftName, companyId)
                .orElseThrow(() -> new ResourceNotFoundException("Shift template with name '" + shiftName + "' does not exist in company with ID " + companyId));

        repo.delete(shiftTemplate);
    }
```
con:
```java
    @Transactional
    public void deleteShiftTemplate(String shiftName, UUID companyId, UUID actorId) {

        log.info("Deleting Shift Template '{}' for company {}", shiftName, companyId);

        String normalizedShiftName = stringUtils.normalizeString(shiftName);

        ShiftTemplate shiftTemplate = repo.findByShiftNameAndCompanyCompanyId(normalizedShiftName, companyId)
                .orElseThrow(() -> new ResourceNotFoundException("Shift template with name '" + shiftName + "' does not exist in company with ID " + companyId));

        User actor = userService.getUserById(actorId);
        SoftDeleteSupport.softDelete(repo, shiftTemplate, actor);
    }
```

- [ ] **Step 3: Aggiungi il principal al controller e all'interfaccia**

In `ShiftTemplateControllerInterface.java`, aggiungi gli import:
```java
import com.pat.crewhive.security.CustomUserDetails;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
```
e sostituisci:
```java
    ResponseEntity<?> deleteShiftTemplate(
            @PathVariable @NotBlank @NoHtml @Size(min = 1, max = 32) String shiftName,
            @PathVariable @NotNull UUID companyId
    );
```
con:
```java
    ResponseEntity<?> deleteShiftTemplate(
            @AuthenticationPrincipal CustomUserDetails cud,
            @PathVariable @NotBlank @NoHtml @Size(min = 1, max = 32) String shiftName,
            @PathVariable @NotNull UUID companyId
    );
```

In `ShiftTemplateController.java`, aggiungi gli import:
```java
import com.pat.crewhive.security.CustomUserDetails;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
```
e sostituisci:
```java
    @Override
    @PreAuthorize("hasRole('MANAGER')")
    @DeleteMapping("/delete/{shiftName}/company/{companyId}")
    public ResponseEntity<?> deleteShiftTemplate(@PathVariable @NotBlank @NoHtml @Size(min = 1, max = 32) String shiftName,
                                                 @PathVariable @NotNull UUID companyId) {

        log.info("Received request to delete shift template '{}' for company ID {}", shiftName, companyId);

        shiftTemplateService.deleteShiftTemplate(shiftName, companyId);

        return ResponseEntity.ok().build();
    }
```
con:
```java
    @Override
    @PreAuthorize("hasRole('MANAGER')")
    @DeleteMapping("/delete/{shiftName}/company/{companyId}")
    public ResponseEntity<?> deleteShiftTemplate(@AuthenticationPrincipal CustomUserDetails cud,
                                                 @PathVariable @NotBlank @NoHtml @Size(min = 1, max = 32) String shiftName,
                                                 @PathVariable @NotNull UUID companyId) {

        log.info("Received request to delete shift template '{}' for company ID {}", shiftName, companyId);

        shiftTemplateService.deleteShiftTemplate(shiftName, companyId, cud.getUserId());

        return ResponseEntity.ok().build();
    }
```

- [ ] **Step 4: Compila e verifica che i test esistenti passino**

Run: `./mvnw test -Dtest=com.pat.crewhive.shifttemplate.**` (se non esistono test per
questo package, esegui almeno `./mvnw compile`)
Expected: PASS / nessun errore di compilazione

- [ ] **Step 5: Commit**

```bash
git add src/main/java/com/pat/crewhive/shifttemplate/ShiftTemplateService.java src/main/java/com/pat/crewhive/shifttemplate/ShiftTemplateController.java src/main/java/com/pat/crewhive/shifttemplate/ShiftTemplateControllerInterface.java
git commit -m "ShiftTemplateService.deleteShiftTemplate: usa SoftDeleteSupport, aggiunge il principal al controller"
```

---

## Task 24: `EventService.deleteEvent` → `SoftDeleteSupport` + attore, rimuove bulk call ridondante

**Files:**
- Modify: `src/main/java/com/pat/crewhive/event/EventService.java`
- Modify: `src/main/java/com/pat/crewhive/event/EventController.java`
- Modify: `src/main/java/com/pat/crewhive/event/EventControllerInterface.java`

**Interfaces:**
- Consumes: `SoftDeleteSupport.softDelete` (Task 2), `Event` (Task 9). `CustomUserDetails`/`AuthenticationPrincipal` già importati in entrambi i file del controller.
- Produces: `EventService.deleteEvent(UUID eventId, UUID actorId)` (firma cambiata).

- [ ] **Step 1: Riscrivi `EventService.deleteEvent`**

Sostituisci:
```java
    @Transactional
    public void deleteEvent(UUID eventId) {

        log.info("Deleting event with ID: {}", eventId);

        if (!eventRepository.existsById(eventId)) {
            throw new ResourceNotFoundException("Evento non trovato con ID: " + eventId);
        }

        eventUsersRepository.deleteByEventId(eventId);
        eventRepository.deleteById(eventId);
    }
```
con:
```java
    @Transactional
    public void deleteEvent(UUID eventId, UUID actorId) {

        log.info("Deleting event with ID: {}", eventId);

        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Evento non trovato con ID: " + eventId));

        User actor = userService.getUserById(actorId);
        SoftDeleteSupport.softDelete(eventRepository, event, actor);
    }
```
(la cascata su `Event.users`, cascade=ALL/orphanRemoval, rende `eventUsersRepository.deleteByEventId`
ridondante: rimosso, non sostituito. Il campo `eventUsersRepository` resta comunque usato
altrove in questa classe, es. `findEventsByUserId`.)

Aggiungi l'import:
```java
import com.pat.crewhive.common.audit.SoftDeleteSupport;
```
(`User` è già importato in `EventService.java`).

- [ ] **Step 2: Thread l'attore da controller e interfaccia**

In `EventControllerInterface.java`, sostituisci:
```java
    ResponseEntity<String> deleteEvent(@PathVariable @NotNull UUID eventId);
```
con:
```java
    ResponseEntity<String> deleteEvent(@AuthenticationPrincipal CustomUserDetails cud, @PathVariable @NotNull UUID eventId);
```

In `EventController.java`, sostituisci:
```java
    @Override
    @DeleteMapping(path = "/delete/{eventId}", produces = "application/json")
    public ResponseEntity<String> deleteEvent(@PathVariable @NotNull UUID eventId) {

        log.info("Received request to delete event with id: {}", eventId);

        eventService.deleteEvent(eventId);
        return ResponseEntity.ok("Evento eliminato con successo");
    }
```
con:
```java
    @Override
    @DeleteMapping(path = "/delete/{eventId}", produces = "application/json")
    public ResponseEntity<String> deleteEvent(@AuthenticationPrincipal CustomUserDetails cud, @PathVariable @NotNull UUID eventId) {

        log.info("Received request to delete event with id: {}", eventId);

        eventService.deleteEvent(eventId, cud.getUserId());
        return ResponseEntity.ok("Evento eliminato con successo");
    }
```

- [ ] **Step 3: Esegui la suite event per non aver rotto nulla**

Run: `./mvnw test -Dtest=EventServiceTest`
Expected: PASS (nessun test esistente copriva `deleteEvent`, verificato prima di scrivere questa task)

- [ ] **Step 4: Commit**

```bash
git add src/main/java/com/pat/crewhive/event/EventService.java src/main/java/com/pat/crewhive/event/EventController.java src/main/java/com/pat/crewhive/event/EventControllerInterface.java
git commit -m "EventService.deleteEvent: usa SoftDeleteSupport, riceve l'attore dal controller"
```

---

## Task 25: `ShiftProgrammedService.deleteShift` → `SoftDeleteSupport`, rimuove bulk call ridondante

**Files:**
- Modify: `src/main/java/com/pat/crewhive/shiftprogrammed/ShiftProgrammedService.java`

**Interfaces:**
- Consumes: `SoftDeleteSupport.softDelete` (Task 2), `ShiftProgrammed` (Task 8).

`requesterUserId` è già un parametro del metodo: fa direttamente da attore, nessuna
modifica al controller necessaria.

- [ ] **Step 1: Riscrivi `deleteShift`**

Sostituisci:
```java
    public void deleteShift(
            UUID requesterUserId,
            UUID shiftId) {

        log.info("deleteShift: Deleting shift with id: {}", shiftId);

        if (!shiftProgrammedRepository.existsById(shiftId)) {

            log.error("deleteShift: Shift with id {} does not exist", shiftId);
            throw new ResourceNotFoundException("Shift not found with ID: " + shiftId);
        }

        shiftUserRepository.deleteByShiftId(shiftId);
        shiftProgrammedRepository.deleteById(shiftId);
    }
```
con:
```java
    public void deleteShift(
            UUID requesterUserId,
            UUID shiftId) {

        log.info("deleteShift: Deleting shift with id: {}", shiftId);

        ShiftProgrammed shift = shiftProgrammedRepository.findById(shiftId)
                .orElseThrow(() -> new ResourceNotFoundException("Shift not found with ID: " + shiftId));

        User actor = userService.getUserById(requesterUserId);
        SoftDeleteSupport.softDelete(shiftProgrammedRepository, shift, actor);
    }
```
(la cascata su `ShiftProgrammed.users`, cascade=ALL/orphanRemoval, rende
`shiftUserRepository.deleteByShiftId` ridondante: rimosso, non sostituito. Il campo
`shiftUserRepository` resta comunque usato altrove in questa classe.)

Aggiungi l'import:
```java
import com.pat.crewhive.common.audit.SoftDeleteSupport;
```
(`User` è già importato in `ShiftProgrammedService.java`).

- [ ] **Step 2: Esegui la suite per non aver rotto nulla**

Run: `./mvnw test -Dtest=ShiftProgrammedServiceTest`
Expected: PASS (nessun test esistente copriva `deleteShift`, verificato prima di scrivere questa task)

- [ ] **Step 3: Commit**

```bash
git add src/main/java/com/pat/crewhive/shiftprogrammed/ShiftProgrammedService.java
git commit -m "ShiftProgrammedService.deleteShift: usa SoftDeleteSupport"
```

---

## Task 26: Verifica finale

**Files:** nessuno (solo verifica).

**Interfaces:**
- Consumes: tutto il lavoro delle task 1-25.
- Produces: conferma che il modulo compila e l'intera suite passa.

- [ ] **Step 1: Compila l'intero progetto**

Run: `./mvnw compile`
Expected: BUILD SUCCESS, nessun errore

- [ ] **Step 2: Esegui l'intera suite di test**

Run: `./mvnw test`
Expected: BUILD SUCCESS, tutti i test verdi (i test che richiedono Postgres/Redis
raggiungibili, come `HoursCalculatorApplicationTests`, restavano già esclusi/falliti
prima di questo lavoro in un ambiente sandboxed: non è una regressione introdotta qui —
verifica solo che non ci siano NUOVI fallimenti rispetto allo stato pre-piano).

- [ ] **Step 3: Se tutto verde, non serve un commit — il lavoro è già tutto committato task per task**

Se emergono fallimenti non anticipati in questa task, torna alla task che ha introdotto
il file coinvolto e correggi lì, poi ripeti Step 1-2.
