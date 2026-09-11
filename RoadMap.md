# CrewHive Server — Valutazione complessiva + roadmap di rientro

## Context

Richiesta: valutazione complessiva "momentanea" del progetto CrewHive Server (backend Spring Boot
4.1 / Java 25 per gestione turni, eventi e presenze aziendali), con in più:
- giudizio d'insieme ("cosa ne pensi");
- falle di sicurezza gravi;
- cosa è già ottimo;
- come si colloca il backlog di issue aperte (#8–#25) ancora da fare.

L'analisi è stata fatta su 3 assi (architettura/auth, sicurezza, dati/test/ops) leggendo tutto
`src/main`, i `application*.properties`, `docker-compose.yml`, `Dockerfile`, `logback-spring.xml`,
`documentazione.md`, `todo_list.md` e la history git. I finding critici sono stati verificati
direttamente sui file.

Questo documento è **una valutazione + un ordine di lavoro proposto**, non un intervento di codice.
Nessuna modifica è stata fatta.

---

## 1. Giudizio d'insieme

**Progetto solido come impianto, ma non ancora pronto per essere venduto/deployato in multi-tenant.**

Il livello di ingegneria del *singolo modulo* è buono: layering pulito
(Controller → Service → Repository), DTO come `record` con Bean Validation, gestione errori
RFC 7807, header di sicurezza HTTP ben fatti, JWT asimmetrico RS256 con chiavi esterne
(AWS Parameter Store o env), soft-delete uniforme con audit, attenzione all'N+1.

Il problema non è la qualità artigianale delle classi: è che **il modello multi-tenant non esiste
davvero**. C'è un'entità `Company`, ma non c'è isolamento del tenant. La security si ferma a
`.anyRequest().authenticated()` e i controlli "questo utente può toccare questo oggetto?" sono
applicati a mano, a macchia di leopardo, nel service layer — e per gli endpoint che scrivono
ore/turni/eventi non ci sono affatto. Per un prodotto che gestisce **ore e straordinari** (cioè
in ultima analisi la busta paga), questo è bloccante.

Valutazione sintetica (1–5):

| Aspetto | Voto | Note |
|---|:--:|---|
| Architettura / struttura codice | 3.5 | pulita ma con god-service, package `security` tuttofare, URL non versionati |
| Modello dati / JPA | 3 | soft-delete curato; manca `@Version` sui saldi utente, indice fantasma `username`, `ContractJSON` morto |
| **Sicurezza / autorizzazione** | **1.5** | IDOR sistematici, payroll tampering, refresh token in chiaro, nessun rate limit |
| Auth (JWT/refresh) | 3 | RS256 ok; ma alg non pinnato, token vivi dopo disattivazione, refresh non hashati |
| Persistenza / migrazioni | 2 | `ddl-auto=update` su tutti i profili, zero migrazioni versionate |
| Caching Redis | 2 | usato solo in 2 service, chiavi non tenant-scoped, evict incompleti, nessun fallback |
| Test | 2 | solo unit Mockito sui service; 0 test di controller/repository/security/cron; l'unico `@SpringBootTest` non asserisce nulla |
| Logging / osservabilità | 1.5 | `logback-spring.xml` rotto, `logs/app.log` committato, claims JWT (PII) loggati a INFO ad ogni request |
| Deploy / ops | 2 | Docker gira come root, credenziali DB deboli in compose, nessuna CI |
| Documentazione | 3.5 | `documentazione.md` molto dettagliata ma già in parte disallineata (ID `Long` vs `UUID`) |

**In una frase:** ottimo scheletro, ma prima di venderlo servono (a) l'autorizzazione a livello di
oggetto/tenant su *ogni* endpoint, (b) migrazioni versionate, (c) un minimo di test di
integrazione e (d) logging sano.

---

## 2. Cosa è già OTTIMO (da non toccare)

1. **Header di sicurezza HTTP** (`SecurityConfig.java:76-101`): CSP restrittiva, HSTS 1 anno +
   subdomains, Referrer-Policy `no-referrer`, Permissions-Policy, COOP/CORP `same-origin`,
   `X-Content-Type-Options`. Fatto meglio di tanti progetti in produzione.
2. **JWT asimmetrico RS256 con chiavi fuori dal codice**: private key mai nel repo, mai
   nell'immagine Docker; doppia strategia `cloud` (AWS SSM Parameter Store) / `onpremise` (env).
   `JwtKeyConfig` fa fail-fast se il profilo non è valido. Design corretto per una futura edizione
   self-hosted.
3. **Niente SQL injection**: tutte le `@Query` / native usano parametri nominati, nessuna
   concatenazione di stringhe, nessun `createQuery` concatenato.
4. **Sanitizzazione input anti-XSS**: validator custom `@NoHtml` (jsoup `Safelist.none()`) applicato
   sui campi stringa di event/shift/role/registration, `description` inclusa. jsoup 1.23.1 aggiornato
   (vuln risolta nel commit `ec48676`).
5. **Soft-delete uniforme e curato**: `SoftDeletableEntity` + `@SQLRestriction`/`@SQLDelete` su 12
   entità, con `deletedAt`/`deletedBy`, bind dell'optimistic-lock nel delete delle entità
   `@Version`, riattivazione dei join-row invece di duplicare. Molti bug già trovati e corretti
   (commit `2572b38`, `c027853`, `98c12da`, `fdacb44`).
6. **Attenzione all'N+1**: `@EntityGraph` mirati su `EventRepository` / `ShiftProgrammedRepository`.
7. **Gestione errori centralizzata** RFC 7807 (`GlobalExceptionHandler` + entry-point/access-denied
   JSON), niente stack trace verso il client.
8. **Password**: BCrypt via encoder standard, `updatePassword` richiede la vecchia password,
   policy di complessità Unicode-aware.
9. **CSRF disabilitato** è la scelta giusta per un'API pura `Authorization: Bearer` senza cookie.
10. **`RedisCacheConfig`**: `BasicPolymorphicTypeValidator` con allow-list di package — mitigazione
    esplicita contro gadget di deserializzazione. Dettaglio maturo.

---

## 3. FALLE DI SICUREZZA GRAVI

### Problema sistemico
`SecurityConfig` autorizza solo `.anyRequest().authenticated()` (`SecurityConfig.java:72`).
"Autenticato" = un JWT valido di *qualunque* tenant. L'autorizzazione a livello di oggetto è poi
applicata in modo incoerente nel service layer: le GET company-scoped controllano l'appartenenza
(`CompanyAccessService.isNotPartOfCompany`), ma **ogni endpoint che prende un `userId` / `eventId` /
`shiftId` / `companyId` per una lettura o scrittura sul singolo oggetto NON lo fa**.

### CRITICI (verificati sul codice)

**C1 — Qualunque utente autenticato può gonfiare ore e straordinari di chiunque (frode busta paga)**
`ShiftWorkedController.java:25-34` → `ShiftWorkedService.java:36-61`.
`POST /shift-worked/create` non ha `@PreAuthorize`. Il body porta `userId` e `extraHours`
(`CreateShiftWorkedDTO.java:29-33`, e `extraHours` ha solo `@NotNull`: nessun `@Positive`/`@Min`,
quindi accetta anche valori negativi). Il service carica quell'utente arbitrario e fa
`user.setOvertimeHours(old.add(dto.extraHours()))`. Nessun ruolo, nessun check di stesso tenant,
nessun check "sono io".
*Exploit:* un USER qualsiasi fa `POST /shift-worked/create` con `{"userId":"<chiunque>","extraHours":999,...}`.

**C2 — Qualunque utente autenticato può modificare/cancellare qualsiasi evento per ID (cross-tenant)**
`EventController.java:72-89` → `EventService.java:167-240`.
`PATCH /event/patch` non riceve nemmeno il principal: `patchEvent(PatchEventDTO)` usa solo
`dto.eventId()` e riscrive nome/date/colore/tipo e **il set dei partecipanti** (user ID arbitrari).
`DELETE /event/delete/{eventId}` usa il caller solo come "attore" dell'audit, non per autorizzare.

**C3 — Qualunque utente autenticato può modificare/cancellare qualsiasi turno programmato per ID**
`ShiftProgrammedController.java:72-91` → `ShiftProgrammedService.java:231-329`.
`patchShift` / `deleteShift` caricano il turno solo per `dto.shiftProgrammedId()` / `shiftId`;
`cud.getUserId()` serve solo come chiave di cache-evict e attore del soft-delete. Si possono anche
riassegnare le persone dentro/fuori dai turni di un altro tenant.

### ALTI

**H1 — Disclosure cross-tenant di agenda e turni altrui.**
`GET /event/user/{userId}`, `GET /event/{temp}/user/{userId}`,
`GET /shift-programmed/period/{p}/user/{userId}`, `GET /shift-programmed/users/{shiftId}`: nessun
controllo, `userId`/`shiftId` arbitrari, ritornano date e nomi dei colleghi.

**H2 — Disclosure cross-tenant di PII/HR via CompanyController.**
`CompanyService.getCompanyUserWithInformation` (`CompanyService.java:171-201`) verifica che il
*manager* appartenga a `companyId`, ma non che `targetId` appartenga a `companyId`. Un manager legge
email, contratto, ore, saldi ferie/permessi di qualsiasi utente di qualsiasi azienda.

**H3 — `updateUserRole` non verifica che il target sia nella company del manager**
(`RoleService.java:75-88`): un manager di A può assegnare ruoli di A a utenti di B. In più
`UpdateUserRoleDTO.newRole` ha `@Min/@Max` su una `String` → Bean Validation invalida → 500.

**H4 — Gli endpoint ShiftTemplate si fidano del `companyId` fornito dall'attaccante.**
Tutti `@PreAuthorize("hasRole('MANAGER')")` (autorità *globale*), ma `companyId` arriva da
path/body e non è mai confrontato con la company del chiamante (`ShiftTemplateService`). Un manager
qualsiasi fa CRUD sui template di un'altra azienda.

**H5 — Refresh token in chiaro nel DB, nessuna reuse-detection.**
`RefreshToken.token` = `UUID.randomUUID().toString()` salvato così com'è (`RefreshTokenService.java:34-49`).
Rotazione in-place senza invalidazione della "famiglia": un token rubato e usato causa solo un 404
silenzioso al legittimo proprietario. Scadenza a granularità di *giorno* (`LocalDate` +15gg).
Chi legge la tabella (backup, log, altra vuln) ha token immediatamente spendibili su
`POST /api/auth/rotate` (che è `permitAll`).

**H6 — Escalation self-service a MANAGER + assenza di scoping a valle.**
`POST /company/register` richiede solo autenticazione e assegna il ruolo **globale** `ROLE_MANAGER`
(`CompanyService.java:66-107`). Combinato con H4, qualunque utente si promuove e ottiene CRUD
cross-tenant sui template. (Nota: `SecurityConfig` fa `permitAll` su `/api/auth/register/manager`
che **non esiste** come endpoint — regola morta.)

**H7 — Assegnazione di massa di eventi/turni a utenti arbitrari.**
`createEvent` / `createShift` accettano qualsiasi lista di UUID; unico gate è "gli eventi PUBLIC
richiedono ROLE_MANAGER". Calendar spam / molestie / inquinamento dati cross-tenant.

### MEDI (sintesi)
- **M2 — Nessun rate limiting** su login/register/rotate (`permitAll`, non throttled) → credential
  stuffing, brute force, guessing dei refresh token. (= issue #21, confermata.)
- **M3 — Enumerazione account**: messaggi/status diversi per "utente inesistente" vs "password
  errata" vs "account disabilitato" vs "email già registrata".
- **M4 — PII nei log a INFO**: `JwtService.java:91` logga **tutti i claims** (email, nome, cognome,
  companyId, ruoli, jti) **ad ogni request autenticata**; email loggate in auth/refresh/user.
  `logs/app.log` è **committato** e la sua history contiene username di login falliti reali.
- **M5 — I token vivono dopo disattivazione/rimozione/cambio ruolo/logout-altrove**:
  `CustomUserDetails` hard-codea `working=true`; `deleteAccount`/`leaveCompany` cancellano il refresh
  ma non mettono in blacklist l'access token → finestra di 15 min di accesso pieno.
- **M6 — jjwt con versioni disallineate**: `jjwt-api` 0.11.5 + `jjwt-impl` **0.12.6** + `jjwt-jackson`
  0.11.5. Mix non supportato; il codice usa l'API pre-0.12 (`parserBuilder`, `setSigningKey`,
  `setId`) → fragile / rischio compilazione durante la migrazione.
- **M7 — Verifica JWT senza algoritmo pinnato e senza clock-skew**: `parserBuilder().setSigningKey()`
  senza allow-list `RS256`. L'algorithm-confusion è bloccato solo dai check interni di jjwt, non da
  policy esplicita — fragile visto M6.
- **M1 — `setCompany`** autorizza su `companyId` del token ma poi muta la company risolta *per nome*
  dal body (`CompanyService.java:212-239`): un manager può arruolare un utente company-less in
  un'altra azienda.

### BASSI (sintesi)
`show-sql=true` di default in prod (L1); `ddl-auto=update` in prod (L2); ~~Swagger/OpenAPI
world-readable in ogni profilo (L3)~~ **risolto**: `/docs`, `/docs/**` ora richiedono
autenticazione JWT + ruolo `DEV` (`SecurityConfig`/`JwtAuthenticationFilter`); CORS
`allowCredentials(true)` senza `allowedOrigins` +
metodo fittizio `"QUERY"` (L4); credenziali DB `crewhive/crewhive` in `docker-compose.yml` con
Postgres/Redis esposti sull'host, Redis senza auth (L5); `GlobalExceptionHandler` rimanda
`ex.getMessage()` degli `IllegalArgumentException` al client, a volte con identificatori (L6);
`ShiftTemplateController` ritorna l'entity invece di un DTO (L7); policy password incoerente
(`RegistrationDTO` 12–32 vs `PasswordUtil` 8–20 → effettivo 12–20) (L8); `.env` in chiaro sul disco
con AWS key + RSA private key, anche se il profilo attivo è `onpremise` (L9); `/api/auth/rotate`
va in NPE su `cud` null *dopo* aver già ruotato il refresh (L10); outage Redis = outage auth
(fail-closed, ma single point of failure) (L11).

---

## 4. Debito tecnico / problemi non-security principali

1. **Nessuna migrazione versionata** (issue #15). `ddl-auto=update` su *tutti* i profili, seeding
   `event_type` via `ApplicationRunner`. Indice fantasma: `User.java:20` dichiara
   `@Index(... columnList = "username")` ma `User` non ha `username` → su DB pulito la DDL fallisce.
2. **Concorrenza sui saldi utente**: `User` non ha `@Version`, ma `ShiftWorkedService`/`UserService`
   fanno read-modify-write su `overtimeHours`/`vacationDaysAccumulated` → lost update. Già segnalato
   in `documentazione.md:1172`.
3. **Cron accrual non sicuri in multi-istanza** (issue #24 / #4). `MonthlyLeaveDaysCron` e
   `MonthlyVacationCron` girano allo **stesso identico cron** `0 0 2 1 * *`, senza ShedLock, con
   `UPDATE ... col = col + delta` non idempotente → con N repliche gli scatti vengono applicati N
   volte. `MonthlyLeaveDaysCron` logga pure il nome sbagliato ("MonthlyVacationCron eseguito").
4. **Caching Redis incompleto/incoerente** (issue #10). Usato solo in `company` e `shiftprogrammed`.
   `getShiftsByPeriodAndCompany` è keyed per `requesterUserId` invece che per `companyId` → ogni
   utente ha la sua copia e una scrittura evicta solo la chiave dello scrittore (viste stale fino a
   30 min). Evict solo per `DAY/WEEK/MONTH` (mancano `TRIMESTER/SEMESTER/YEAR`). Le mutazioni del
   package `manager` non evictano nulla. `CacheKeys.shiftsByUser` e `shiftsByCompany` sono
   identiche. Nessun `CacheErrorHandler`, nessun profilo `cache=none` per il locale → Redis è
   dipendenza hard senza degradazione.
5. **Test** (issue #9). 25 file, ~2000 righe, ma: solo unit Mockito sui service (6 su ~14);
   0 test di controller, 0 di repository, 0 di cron, quasi 0 sulla security chain; gli 11
   `*SoftDeleteMappingTest` asseriscono via reflection sull'esatta stringa SQL (fragilissimi);
   l'unico `@SpringBootTest` non asserisce nulla e richiede Postgres+Redis vivi. Nessun test gira
   offline in CI.
6. **Logging** (issue #12). `logback-spring.xml` referenzia l'appender inesistente `CALCULATOR_FILE`,
   ha due `<logger name="com.pat.crewhive">` in conflitto, usa `FileAppender` con `append=false`
   (tronca ad ogni avvio), nessuna rotazione, nessun MDC/request-id nonostante il README parli di
   "logging strutturato con tracciabilità". `logs/app.log` è nel repo.
7. **Incoerenze architetturali**: `CompanyService` god-service; `RoleAssignmentService` esiste solo
   per rompere un ciclo di bean; classe main ancora `HoursCalculatorApplication`; URL non versionati
   e misti (`/api/auth` vs `/company`); `GlobalExceptionHandler` mappa `IllegalStateException`
   ("non posso cancellare: ...") a **500** invece che 409.
8. **Codice morto / drift**: `ContractJSON` non è referenziato da nessuna parte (ma `User` usa
   colonne flat); `documentazione.md` descrive ancora ID `Long` e `findByUsername` (rimossi).
9. **`UserPreferences`** ha doppio mapping della stessa relazione (`user_user_id` su `users` +
   `user_id` via `@MapsId`) e non estende `SoftDeletableEntity` (hard-delete su soft-delete
   dell'utente).
10. **`hashCode()` costante `31`** su `EventUsers`/`ShiftUser`/`UserRole` → i `Set` degenerano a
    scansione lineare; `UserRoleId.hashCode()` va in NPE su id null (transient).
11. **Docker**: gira come root, no flag heap/container, `COPY *.jar`, nessun healthcheck. Nessuna CI.

---

## 5. Come si colloca il backlog di issue aperte

| Issue | Tema | Priorità reale | Nota |
|---|---|:--:|---|
| **#23 Audit Log** | — | **P0** (anticipare) | serve *prima* di vendere: oggi non sai chi ha toccato le ore |
| **#21 Rate limiting** | security | **P0** | confermato assente; login/rotate esposti e non throttled |
| **#9 Finire i test** | refactor | **P0** | senza test di integrazione non puoi rifattorizzare la security in sicurezza |
| **#15 Migrazioni DB** | optimization | **P1** | bloccante per il deploy; c'è già un bug DDL latente (`username`) |
| **#18 Debito tecnico dalla doc** | repair | **P1** | 3/7 fatti; contiene fix rapidi ad alto valore |
| **#8 Gestione database** | optimization | **P1** | `ddl-auto`, pool, OSIV, indici parziali su `active` |
| **#12 Log** | optimization | **P1** | `logback` rotto + PII a INFO + file committato = va sistemato subito |
| **#24 Cron eliminazione dati** | optimization | **P1** | serve ShedLock *anche* per i cron accrual già esistenti |
| **#10 Cache Redis** | refactor | **P2** | correttezza (tenant key + evict) prima di estenderne l'uso |
| **#17 Chiavi RSA per istanza** | deployment | **P2** | dipende dall'architettura di #14 |
| **#25 Parametri settabili** | deployment | **P2** | esternalizzare TTL token/refresh, policy password, ecc. |
| **#19 Tenant directory** | refactor | **P2** | dopo che esiste un vero tenant isolation |
| **#14 Architettura deployment SaaS/self-hosted** | deployment | **P2** | grosso; sblocca #17/#19/#25 |
| **#20 Export verso software paghe** | optimization | **P3** | feature; dopo audit log + correttezza ore |
| **#13 Monetizzazione/GTM** | no-code | **P3** | non tecnico |

**Assente dal backlog e più urgente di tutto:** l'autorizzazione a livello di oggetto/tenant su
ogni endpoint (i CRITICI C1–C3 e gli ALTI H1–H7). Va aperta come issue **P0** a sé.

---

## 6. Ordine di lavoro proposto

**Fase 0 — Blocco sicurezza (P0, da fare subito, insieme):**
1. Introdurre un test di integrazione minimo (`@SpringBootTest` + Testcontainers Postgres/Redis, o
   `@WebMvcTest` per la security) — rete di sicurezza per i passi successivi. (issue #9)
2. Autorizzazione a livello di oggetto su **ogni** endpoint:
   - passare sempre il principal ai service `patch`/`delete` (oggi `EventController.patchEvent` non
     lo passa nemmeno);
   - verificare che l'oggetto (`event`/`shift`/`shiftWorked`/`user`/`template`) appartenga alla
     company del chiamante — helper già esistente: `CompanyAccessService.isNotPartOfCompany`;
   - `@PreAuthorize` mancanti su `EventController`, `ShiftProgrammedController`,
     `ShiftWorkedController`, `UserController`;
   - `ShiftWorked`/`update-user-role`/`update-user-work-info`: solo MANAGER *della stessa company*
     del target; `extraHours` → `@PositiveOrZero`.
3. Rate limiting su `/api/auth/**` (bucket4j, storage Redis già presente). (issue #21)
4. Refresh token: hash at rest (SHA-256) + reuse-detection con invalidazione della famiglia;
   scadenza a `Instant` non `LocalDate`. (parte di #25 per i TTL)
5. Log: togliere il dump dei claims (`JwtService.java:91`), rimuovere `logs/` dal repo + `.gitignore`,
   `RollingFileAppender` con `append=true`, fixare l'appender `CALCULATOR_FILE`. (issue #12)
6. Pinnare l'algoritmo JWT a `RS256` + clock-skew; allineare le versioni jjwt. (parte #18)
7. Blacklistare l'access token su `deleteAccount`/`leaveCompany`/cambio ruolo.

**Fase 1 — Fondamenta deploy (P1):**
8. Flyway; `ddl-auto=validate` per `cloud`/`onpremise`, `update` solo in un profilo `local`;
   spostare seed `event_type` e bootstrap ruoli in migrazioni. Correggere l'indice `username`.
   (issue #15, #8)
9. Audit log (append-only, chi/quando/cosa su ore, ruoli, turni, company). (issue #23)
10. ShedLock sui cron; unificare i due cron accrual in un job idempotente; retention per refresh
    token scaduti e soft-deleted vecchi. (issue #24, #4)
11. `@Version` su `User` (o update mirati `col = col + :delta` in query). 
12. Quick win da #18: `IllegalStateException` → 409; `CacheKeys` duplicata; `ContractJSON` morto;
    `RemoveUserFromCompanyOutputDTO` inutilizzato; nome classe main; metodo `NotMatches`.

**Fase 2 — Correttezza cache + architettura tenant (P2):**
13. Cache: chiave per `companyId`, evict per tutti i partecipanti, coprire tutti i `Period`,
    `CacheErrorHandler` log-and-continue, profilo `cache=none` locale. (issue #10)
14. Architettura SaaS multi-tenant vs self-hosted + tenant directory + chiavi per istanza +
    parametri esternalizzati. (issue #14, #17, #19, #25)

**Fase 3 — Feature (P3):**
15. Export verso software paghe (dopo audit log + correttezza ore). (issue #20)

---

## 7. Verifica

Non essendo un intervento di codice, la "verifica" di questa valutazione è la revisione dei finding:
- I CRITICI C1–C3 e la config CORS/security sono stati verificati leggendo direttamente
  `ShiftWorkedController/Service`, `EventController`, `ShiftProgrammedController`, `SecurityConfig`,
  `CreateShiftWorkedDTO`.
- Gli ALTI/MEDI derivano dall'analisi incrociata dei 3 assi; ognuno cita `file:riga` e va
  ricontrollato in fase di fix.
- Prima di iniziare la Fase 0: `mvn -q test` per fotografare lo stato attuale dei test
  (attenzione: l'unico `@SpringBootTest` richiede Postgres + Redis raggiungibili).
