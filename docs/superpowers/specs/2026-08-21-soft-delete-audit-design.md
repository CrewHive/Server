# Soft-delete per audit/legal su tutte le entità di dominio

Data: 2026-08-21
Autore: Pat (con Claude Code)
Stato: approvato per la fase di planning

## 1. Obiettivo

Introdurre un pattern di soft-delete uniforme su tutte le entità di dominio (tranne
quelle effimere) per finalità di audit e obblighi legali: nessuna riga viene più
cancellata fisicamente da un flusso applicativo normale; viene invece marcata come
non attiva, con traccia di quando e da chi.

Questo lavoro nasce da due esigenze emerse in [[jpa-relations-review]]:
- `User.shiftWorked` non deve più cancellare lo storico ore lavorate quando un
  utente viene rimosso (era `cascade=ALL, orphanRemoval=true`, causa di perdita
  dati per audit/payroll).
- Estensione generale del principio "non cancellare mai fisicamente un record di
  business" a tutte le entità che lo rappresentano.

## 2. Scope

### Entità incluse (ricevono i tre campi di audit)
`Company`, `User`, `Event`, `EventUsers`, `Role`, `UserRole`, `ShiftProgrammed`,
`ShiftUser`, `ShiftTemplate`, `ShiftWorked`, `EventTypeEntity`.

### Entità escluse
`RefreshToken` — è uno stato di sessione effimero (già cancellato/ruotato dal suo
stesso ciclo di vita), non un record con valenza di audit.

Include anche le entità-ponte M:N (`EventUsers`, `ShiftUser`, `UserRole`) e la
tabella di lookup `EventTypeEntity`, per uniformità completa su tutto il dominio
(decisione esplicita dell'utente, vedi §6 per le implicazioni tecniche su
`EventUsers`/`ShiftUser`).

## 3. Modello dati: `SoftDeletableEntity`

Nuova `@MappedSuperclass` in un package condiviso (es. `com.pat.crewhive.common.audit`):

```java
@MappedSuperclass
public abstract class SoftDeletableEntity {

    @Column(name = "active", nullable = false)
    private boolean active = true;

    @Column(name = "deleted_at")
    private OffsetDateTime deletedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "deleted_by")
    private User deletedBy;

    public boolean isActive() { return active; }

    public OffsetDateTime getDeletedAt() { return deletedAt; }

    public User getDeletedBy() { return deletedBy; }

    /** Marca l'entità come cancellata, tracciando l'attore. deletedBy può essere
     *  null quando la cancellazione è conseguenza di una cascata strutturale
     *  (non un'azione diretta di un utente). */
    public void markDeleted(User actor) {
        this.active = false;
        this.deletedAt = OffsetDateTime.now();
        this.deletedBy = actor;
    }

    /** Ripristina l'entità a stato attivo. */
    public void restore() {
        this.active = true;
        this.deletedAt = null;
        this.deletedBy = null;
    }
}
```

Ogni entità in scope estende `SoftDeletableEntity` invece di dichiarare i campi
autonomamente. Il campo `active` "semplice" già presente su `User` (aggiunto in un
lavoro precedente) viene rimosso e sostituito da quello ereditato.

Invariante logico: `active == false` implica sempre `deletedAt != null`;
`deletedBy` può restare `null` anche quando `active == false` (cancellazione a
cascata, vedi §5).

`@SQLRestriction`, `@SQLDelete` e gli indici **non** sono ereditabili da una
`@MappedSuperclass` (non è una entità/tabella) e vanno quindi ripetuti su
ciascuna delle 11 entità concrete.

## 4. Filtro automatico delle query: `@SQLRestriction`

Su ogni entità in scope:

```java
@SQLRestriction("active = true")
```

(`org.hibernate.annotations.SQLRestriction`, sostituisce il deprecato `@Where`,
disponibile nella versione di Hibernate ORM usata da Spring Boot 4.1).

Effetto: `findAll`, le query derivate Spring Data, le query JPQL/Criteria e il
caricamento di collezioni/relazioni escludono automaticamente i record inattivi,
senza dover toccare i repository esistenti. Le **query native** (`nativeQuery =
true`) non sono soggette al filtro: sono l'unica via di fuga deliberata per i casi
in cui serve vedere anche i record cancellati (vedi §6 e §7).

## 5. Meccanismo di cancellazione e cascata: `@SQLDelete`

Su ogni entità in scope, con la PK sostituita colonna per colonna. Per le entità
con `@Id` singolo, un solo `?`:

```java
// ShiftWorked
@SQLRestriction("active = true")
@SQLDelete(sql = "UPDATE shift_worked SET active = false, deleted_at = now() WHERE shift_worked_id = ?")
```

Per le entità con `@EmbeddedId` composito (`EventUsers`, `ShiftUser`), Hibernate
lega i parametri posizionalmente nell'ordine delle colonne della PK così come
dichiarate nell'`@Embeddable` — servono quindi tanti `?` quante le colonne che
compongono la chiave:

```java
// EventUsers — EventUsersId ha userId, poi eventId, in quest'ordine
@SQLRestriction("active = true")
@SQLDelete(sql = "UPDATE event_users SET active = false, deleted_at = now() WHERE user_id = ? AND event_id = ?")
```

Questo intercetta ogni `EntityManager.remove()` (quindi `repository.delete(...)`,
sia diretto sia a cascata via `cascade = CascadeType.ALL` / `orphanRemoval =
true`) e lo trasforma in un `UPDATE` invece di un `DELETE` fisico. Non tocca le
bulk JPQL delete (`@Modifying @Query("delete from ...")`, vedi §7).

`@SQLDelete` non può leggere il valore in memoria di `deletedBy` (il suo SQL è
statico, parametrizzato solo sulla PK). Per popolare `deletedBy` sulle entità
cancellate direttamente da un'azione utente, si usa un helper a due passi,
condiviso da tutti i service che eseguono un soft-delete "di primo livello":

```java
public final class SoftDeleteSupport {

    public static <T extends SoftDeletableEntity, ID> void softDelete(
            JpaRepository<T, ID> repo, T entity, User actor) {

        entity.markDeleted(actor);
        repo.save(entity);     // persiste active/deletedAt/deletedBy sulla riga
        repo.delete(entity);   // fa scattare la cascata reale sui figli collegati
    }
}
```

Il secondo `save` implicito dentro `delete()` (via `@SQLDelete`) rieseguirà un
`UPDATE active=false, deleted_at=now()` sulla stessa riga: è ridondante ma
innocuo, e non sovrascrive `deletedBy` (quella colonna non è nella SQL statica di
`@SQLDelete`).

I figli raggiunti per cascata (es. `User` → `EventUsers`/`ShiftUser`/`UserRole`,
`Role` → `UserRole`, `Event`/`ShiftProgrammed` → le loro associazioni) vengono
così disattivati automaticamente tramite il loro stesso `@SQLDelete`, ma con
`deletedBy = null`: la cancellazione è implicita ("sparito il genitore"), non
un'azione diretta tracciabile su un attore specifico.

Le entità di primo livello (`Company`, `User`, `Event`, `Role`, `ShiftProgrammed`,
`ShiftTemplate`, `ShiftWorked`) usano sempre `SoftDeleteSupport.softDelete(...)`
dal service layer, mai una `repository.delete(...)` diretta.

## 6. Entità-ponte: chiave primaria e riattivazione

`EventUsers` e `ShiftUser` usano `@EmbeddedId` come **chiave primaria**, non solo
come vincolo unique. Una riga soft-deleted continua quindi a occupare quella PK:
un `new EventUsers(user, event)` seguito da persist, per un legame già esistito
(anche se cancellato in passato), fallirebbe per violazione di chiave primaria,
non solo del vincolo `uc_eventusers_event_id`.

`Event.addUser` / `ShiftProgrammed.addUser` cambiano quindi comportamento:
prima di creare una nuova riga, cercano se esiste già una riga (anche inattiva)
con la stessa PK, tramite una query nativa dedicata nel repository che bypassa
`@SQLRestriction`:

```java
// EventUsersRepository
@Query(value = "SELECT * FROM event_users WHERE event_id = :eventId AND user_id = :userId", nativeQuery = true)
Optional<EventUsers> findByIdIncludingDeleted(@Param("eventId") UUID eventId, @Param("userId") UUID userId);
```

Se la riga esiste ed è inattiva: viene riattivata (`restore()`) e riaggiunta alla
collezione in memoria, invece di costruirne una nuova. Se non esiste: si procede
come oggi.

Questo sposta `addUser` da "metodo puro sull'entità" a operazione che richiede
accesso al repository: la logica di ricerca/riattivazione va quindi nel service
layer (`EventService`, `ShiftProgrammedService`), che poi delega all'entità la
sola manipolazione della collezione in memoria una volta risolto il caso.
`UserRole` non ha lo stesso problema in pratica: un utente ha una sola riga
`UserRole` per via del `@OneToOne` con `@MapsId` sullo `userId`, quindi il caso
"stesso legame ricreato" coincide con "riassegnare un ruolo", già gestito da
`user.setRole(new UserRole(...))` — anche qui va aggiunta la stessa logica di
ricerca/riattivazione per evitare la violazione di PK.

## 7. Query bulk esistenti da riscrivere

Tre repository method attuali sono `@Modifying @Query("delete from ...")`, bulk
JPQL che bypassano il ciclo di vita dell'entità (quindi anche `@SQLDelete`):

- `EventUsersRepository.deleteByEventId`
- `ShiftUserRepository.deleteByShiftId`
- `ShiftUserRepository.deleteByUserId`

Vanno riscritte come update bulk, per restare coerenti con tutto il resto:

```java
@Modifying(clearAutomatically = true, flushAutomatically = true)
@Query("update ShiftUser su set su.active = false, su.deletedAt = :now where su.user.userId = :userId and su.active = true")
int deleteByUserId(@Param("userId") UUID userId, @Param("now") OffsetDateTime now);
```

`deletedBy` resta `null` anche qui (pulizia strutturale/bulk, nessun attore
specifico), coerente con §5.

## 8. Impatto sul login

Con `@SQLRestriction("active = true")` su `User`, `userRepository.findByEmail`
non troverà più gli utenti disattivati: il controllo esplicito `!user.isActive()`
aggiunto in `AuthService.login()` diventerebbe irraggiungibile (la riga non
arriva nemmeno al metodo).

Per mantenere il messaggio distinto "Account disabled" (richiesto esplicitamente,
invece del generico "User not found"), si aggiunge una query nativa di sola
verifica, usata solo quando `findByEmail` non trova nulla:

```java
// UserRepository
@Query(value = "SELECT EXISTS(SELECT 1 FROM users WHERE email = :email AND active = false)", nativeQuery = true)
boolean existsInactiveByEmail(@Param("email") String email);
```

`AuthService.login()`:
```java
User user = userRepository.findByEmail(normalizedEmail)
        .orElseGet(() -> {
            if (userRepository.existsInactiveByEmail(normalizedEmail)) {
                throw new BadCredentialsException("Account disabled");
            }
            throw new ResourceNotFoundException("User not found");
        });
```
(sostituisce sia `userService.getUserByEmail` sia il vecchio controllo
`isActive()` dentro `login()`, che viene rimosso perché non più raggiungibile.)

## 9. Indici

Per ciascuna delle 11 tabelle in scope, tre indici B-tree aggiuntivi nel relativo
`@Table(indexes = {...})`, stesso stile già in uso nel codebase:

```java
@Index(name = "idx_<tabella>_active", columnList = "active"),
@Index(name = "idx_<tabella>_deleted_at", columnList = "deleted_at"),
@Index(name = "idx_<tabella>_deleted_by", columnList = "deleted_by")
```

## 10. Impatto sui flussi esistenti

- **`UserService.deleteAccount`**: sostituisce il soft-delete manuale appena
  scritto (`user.setActive(false); userRepository.save(user);`) con
  `SoftDeleteSupport.softDelete(userRepository, user, user)` (l'attore è
  l'utente stesso), mantenendo la revoca del refresh token prima della
  chiamata.
- **`UserService.leaveCompany`**: la `shiftUserRepository.deleteByUserId(...)`
  sottostante diventa un soft-delete bulk (§7); il distacco da `Company`
  (`user.setCompany(null)`) resta un update normale, non una cancellazione.
- **`CompanyService.removeUserFromCompany(userId, managerId, companyId)`**: oggi
  non passa `managerId` a `userService.leaveCompany`. Resta così per questo
  lavoro (i record toccati da questo flusso restano con `deletedBy = null`,
  coerente con §5 come cancellazione strutturale) — threadare l'attore fin qui
  è fuori scope, valutabile in un secondo momento se si vuole tracciare anche
  *chi* fra i manager ha rimosso l'utente.
- **`Event.removeUser` / `ShiftProgrammed.removeUser`**: nessuna modifica al
  codice chiamante; la rimozione dalla collezione con `orphanRemoval` produce
  ora un soft-delete invece di un hard-delete grazie a `@SQLDelete` (§5).
- **Company/Role/ShiftTemplate/Event/ShiftProgrammed**: al momento non esistono
  service method dedicati a una loro cancellazione diretta oltre a quelli già
  citati; qualunque nuovo endpoint di cancellazione per queste entità dovrà
  usare `SoftDeleteSupport.softDelete(...)`, mai `repository.delete(...)`
  diretta.

## 11. Rischi noti / attenzioni

- **Riferimenti "orfani" verso entità disattivate**: `@SQLRestriction` sul lato
  "uno" di una relazione (es. `EventTypeEntity` referenziato da molti `Event`
  via `@ManyToOne`, `Company` referenziata da `User`/`Role`/`ShiftTemplate`,
  `Role` referenziata da `UserRole`) nasconde la riga anche quando è ancora
  referenziata da entità attive: caricare quella relazione da un record ancora
  attivo può restituire `null`/proxy non risolvibile invece di un errore
  esplicito. Il design non introduce automaticamente un controllo "non puoi
  disattivare X finché esistono riferimenti attivi": è lasciato alla disciplina
  di chi implementerà i singoli endpoint di cancellazione per queste entità,
  da valutare caso per caso quando verranno effettivamente esposti.
- **`ddl-auto=update`**: le nuove colonne verranno aggiunte alle tabelle
  esistenti; su un DB con dati già presenti, `active` va popolato a `true` di
  default (il default a livello Java non retroagisce su righe già esistenti —
  serve che la colonna abbia un default DB-level compatibile, che Hibernate
  genera da `@Column(nullable = false)` con inizializzatore solo per le nuove
  insert; per righe preesistenti Hibernate/Postgres richiede un default
  esplicito in fase di aggiunta colonna, altrimenti l'`ALTER TABLE ADD COLUMN
  ... NOT NULL` fallisce su tabelle non vuote). Da verificare in fase di
  implementazione se il DB attuale ha già righe, ed eventualmente gestire il
  valore di default esplicitamente.
- **Test esistenti**: eventuali test che oggi verificano una cancellazione
  fisica (conteggio righe, `existsById` che torna `false`, ecc.) su una
  qualunque delle 11 entità andranno aggiornati per riflettere la nuova
  semantica (riga presente ma `active = false`).

## 12. Fuori scope

- Anonimizzazione dei dati personali (email, nomi) alla disattivazione: **non
  richiesta** esplicitamente per `User` (l'email può servire), e non prevista
  per nessun'altra entità in questo lavoro.
- Filtraggio delle liste "attivi" negli endpoint esistenti (es.
  `getAllUsersInCompany`) oltre a quanto già garantito automaticamente da
  `@SQLRestriction`: nessun cambiamento aggiuntivo di UX/business logic previsto
  qui, il filtro è già automatico grazie a `@SQLRestriction`.
- Introduzione di uno strumento di migration (Flyway/Liquibase): si resta su
  `ddl-auto=update`, vedi rischio in §11.
- Un "cestino"/vista amministrativa per consultare o ripristinare record
  cancellati: il design lascia la via di fuga tecnica (query native, metodo
  `restore()`), ma non implementa endpoint o UI dedicati.
