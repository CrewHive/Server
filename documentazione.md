# Documentazione Tecnica — CrewHive Server

> Documentazione generata tramite analisi statica del codice sorgente (lettura integrale di ogni classe). Ogni sezione descrive una feature del dominio applicativo, con il dettaglio di ogni classe e di ogni metodo (pubblico, e privato dove rilevante per capire la logica). Le note "Nota"/incoerenze segnalate riportano osservazioni fatte leggendo il codice reale, non ipotesi.

## Panoramica del progetto

**CrewHive** è il backend RESTful della piattaforma omonima, dedicato alla gestione di turni, eventi e presenze in contesti aziendali (ospedali, ristoranti, bar, ecc. — vedi `CompanyType`). Fornisce servizi a client mobile e web per manager e dipendenti, centralizzando autenticazione, autorizzazione e orchestrazione del dominio.

### Stack tecnologico

- **Java 25 + Spring Boot 4.1.0**, architettura a livelli (Controller → Service → Repository).
- **PostgreSQL** con **JPA/Hibernate** per la persistenza (driver `org.postgresql:postgresql`).
- **Hypersistence Utils** (`io.hypersistence:hypersistence-utils-hibernate-73`) per la persistenza di colonne `jsonb` (es. `AddressJSON`, `ContractJSON`).
- **Redis** (`spring-boot-starter-data-redis` + `spring-boot-starter-cache`) come cache distribuita per query costose (turni, aziende).
- **Spring Security** + **JJWT** (`io.jsonwebtoken`) per autenticazione stateless basata su JWT firmati **RSA (RS256)**.
- **Jakarta Validation** per la validazione dei DTO in ingresso, con un vincolo custom `@NoHtml` (basato su **Jsoup**) per la sanificazione anti-XSS.
- **Spring Mail** (`spring-boot-starter-mail`) — presente come dipendenza, non ispezionato in dettaglio nei package analizzati.
- **AWS Parameter Store** (`spring-cloud-aws-starter-parameter-store`) per la gestione di configurazioni/secret esterni.
- **springdoc-openapi** per la documentazione API (Swagger UI) con schema di sicurezza Bearer JWT.
- **Spring Boot Actuator** per health check ed endpoint operativi.
- **Maven** (con wrapper) per build e gestione dipendenze.

### Struttura a package (per feature)

Il codice è organizzato per feature/dominio sotto `com.pat.crewhive`:

| Package                 | Responsabilità                                                                                                                   |
|-------------------------|----------------------------------------------------------------------------------------------------------------------------------|
| `security`              | Autenticazione JWT, filtri, configurazione Spring Security, gestione chiavi RSA, sanificazione input, gestione eccezioni globale |
| `authuser`              | Login, registrazione, refresh/rotazione token, logout                                                                            |
| `user`                  | Anagrafica utente, password, ferie/permessi/straordinari, cron mensili di maturazione                                            |
| `common`                | Utility trasversali (date, stringhe, password, DTO condivisi)                                                                    |
| `company`               | Gestione aziende, associazione utenti, controllo accessi per azienda                                                             |
| `manager`               | Gestione ruoli custom per azienda e aggiornamento dati di lavoro degli utenti                                                    |
| `event`                 | Eventi di calendario (pubblici/privati) con partecipanti                                                                         |
| `shifttemplate`         | Modelli di turno riutilizzabili per azienda                                                                                      |
| `shiftprogrammed`       | Turni pianificati (calendario futuro), con cache Redis                                                                           |
| `shiftworked`           | Turni effettivamente lavorati (consuntivo ore/straordinari)                                                                      |
| `api/swagger`, `config` | Configurazione OpenAPI e cache Redis                                                                                             |

Ogni feature segue tipicamente il pattern: **Entity JPA** → **Repository** (Spring Data JPA) → **Service** (logica di business, `@Transactional`) → **Controller** (`@RestController`) + **ControllerInterface** (annotazioni Swagger separate dall'implementazione) → **DTO** di richiesta/risposta con validazione Bean Validation.

---

## Feature: Sicurezza (security)

Il package `com.pat.crewhive.security` contiene tutta l'infrastruttura di autenticazione/autorizzazione dell'applicazione: generazione e validazione dei JWT (firmati con RSA), il filtro che li intercetta ad ogni richiesta, la configurazione di Spring Security (catena filtri, CORS, security headers), la gestione delle chiavi RSA lette da file PEM, la gestione centralizzata delle eccezioni in formato `ProblemDetail` (RFC 7807) e la sanificazione anti-XSS degli input tramite Jsoup.

### Servizi Core

#### `com.pat.crewhive.security.CustomUserDetails`

**Tipo:** Modello di dominio / implementazione di `org.springframework.security.core.userdetails.UserDetails`.

**Scopo:** Adatta i dati dell'utente (estratti dai claim del JWT, non da un caricamento JPA) al modello richiesto da Spring Security per popolare il `SecurityContext`. Il commento in testa alla classe chiarisce esplicitamente che questa versione non dipende più da entità JPA "lazy": tutti i campi sono valori scalari immutabili copiati dal token, non riferimenti a entità persistite.

Campi (tutti `private final`): `userId` (Long), `email`, `firstName`, `lastName`, `role`, `companyId` (Long), `working` (boolean), più `authorities` (List<GrantedAuthority>) calcolata nel costruttore.

**Metodi pubblici:**

- `CustomUserDetails(Long userId, String email, String firstName, String lastName, String role, Long companyId, boolean working)` — costruttore che valorizza tutti i campi e costruisce `authorities` come lista singola `List.of(new SimpleGrantedAuthority(role))`. **Nota importante**: a differenza di quanto suggerisce il commento Javadoc del metodo `getAuthorities()` ("trasformiamo il campo role... in ROLE_USER"), il codice **non** effettua alcuna trasformazione/prefissazione di `role` con `"ROLE_"` — usa il valore di `role` così com'è. Questo implica che il valore memorizzato nel claim JWT `role` deve già contenere il prefisso `ROLE_` (coerente con il commento in `JwtService.generateToken`: `// ROLE_USER, ROLE_MANAGER, ...`) altrimenti `hasAuthority("ROLE_...")` o `hasRole("...")` di Spring Security non troverebbero corrispondenza.
- `static CustomUserDetails fromClaims(Long userId, String email, String firstName, String lastName, String role, Long companyId)` — factory statica di comodo che chiama il costruttore principale forzando `working = true` (sempre, indipendentemente da come sia impostato realmente lo stato "in servizio" dell'utente sul DB — il filtro JWT infatti usa sempre questo metodo, quindi lo stato `enabled` risulta sempre `true` per ogni token valido).
- `String getUsername()` — override che ritorna `firstName + " " + lastName` come nome utente "logico" ai fini di Spring Security (non è l'email né uno username univoco).
- `String getPassword()` — ritorna sempre `null`: coerente con un modello stateless basato su JWT dove la password non serve dopo l'autenticazione iniziale.
- `Collection<? extends GrantedAuthority> getAuthorities()` — ritorna la lista `authorities` calcolata nel costruttore (singolo ruolo).
- `boolean isAccountNonExpired()` — ritorna sempre `true` (nessuna logica di scadenza account).
- `boolean isAccountNonLocked()` — ritorna sempre `true` (nessuna logica di blocco account).
- `boolean isCredentialsNonExpired()` — ritorna sempre `true`.
- `boolean isEnabled()` — ritorna il campo `working`. Il commento nel codice segnala esplicitamente che, se si volesse disabilitare l'accesso agli utenti non "in servizio", basterebbe far ritornare `working`; attualmente, essendo `fromClaims` a impostare sempre `working = true`, questo controllo è di fatto inattivo in pratica.
- `Long getUserId()`, `String getEmail()`, `String getFirstName()`, `String getLastName()`, `String getRole()`, `Long getCompanyId()` — semplici getter dei campi corrispondenti, usati dalle utility (`UserUtils`) e da altri componenti applicativi per leggere i dati dell'utente autenticato.

**Implicazioni di sicurezza:** la classe non contiene mai password in chiaro né hash; l'intera identità dell'utente proviene dal token JWT firmato e verificato da `JwtService`. Non essendoci verifica live contro il DB (nessun accesso lazy), un token valido ma "stantio" (es. ruolo cambiato dopo l'emissione) continuerà a produrre un `CustomUserDetails` con i vecchi dati fino alla scadenza del token (15 minuti, vedi `JwtService`).

---

#### `com.pat.crewhive.security.JwtService`

**Tipo:** Service Spring (`@Service`) — cuore della logica JWT (generazione e validazione).

**Scopo:** Genera token JWT firmati con chiave privata RSA e valida/decodifica token firmati verificandoli con la chiave pubblica RSA corrispondente. Le chiavi (`PrivateKey`, `PublicKey`) sono iniettate via costruttore (bean definiti in `JwtKeyConfig`).

**Costruttore:** `JwtService(PrivateKey privateKey, PublicKey publicKey)` — salva le due chiavi nei campi `final` e logga (`log.info`) l'avvenuta inizializzazione.

**Metodi pubblici:**

- `String generateToken(Long userId, String email, String firstName, String lastName, String role, Long companyId)`:
  - Costruisce il token con la libreria `io.jsonwebtoken` (JJWT), usando `Jwts.builder()`.
  - **Subject** (`sub`): `String.valueOf(userId)` — l'id numerico dell'utente convertito a stringa.
  - **Claim custom `role`**: valore passato direttamente (atteso già nel formato `ROLE_XXX`, vedi commento nel codice).
  - **Claim custom `email`**, **`firstName`**, **`lastName`**, **`companyId`**: copiati così come ricevuti, senza alcuna sanificazione/validazione aggiuntiva a questo livello (la sanificazione anti-HTML avviene a monte, sui DTO in ingresso, tramite `@NoHtml`).
  - **`iat` (issued at)**: `new Date(System.currentTimeMillis())` — istante di creazione del token.
  - **`exp` (expiration)**: `new Date(System.currentTimeMillis() + 1000 * 60 * 15)` — il token scade **15 minuti** dopo l'emissione. Non è presente alcun claim di tipo `jti` (identificativo univoco del token) né alcuna gestione di revoca/blacklist: la sicurezza contro l'uso di token rubati si basa esclusivamente sulla breve durata di vita.
  - **Firma**: `.signWith(privateKey, SignatureAlgorithm.RS256)` — l'algoritmo usato è **RS256** (RSA + SHA-256), un algoritmo asimmetrico: la firma viene apposta con la chiave privata e può essere verificata da chiunque possieda solo la chiave pubblica, senza poter forgiare nuovi token (a differenza di HMAC dove firma e verifica condividono lo stesso segreto).
  - Il token compattato (`.compact()`) viene poi loggato per intero con `log.info("Generated JWT token: {}", jwt)` **in chiaro nei log applicativi**: questa è una potenziale implicazione di sicurezza da segnalare, poiché espone il token (quindi un bearer credential valido) nei log, che potrebbero essere meno protetti del canale HTTPS di trasporto.
  - Ritorna la stringa del token compattato (formato standard `header.payload.signature`, Base64URL).

- `Claims validateToken(String token)`:
  - Usa `Jwts.parserBuilder().setSigningKey(publicKey).build().parseClaimsJws(token).getBody()` per: (1) verificare la firma RS256 del token con la chiave pubblica, (2) verificare automaticamente la scadenza (`exp`) — la libreria JJWT lancia `ExpiredJwtException` se il token è scaduto, (3) restituire i `Claims` (il payload decodificato) se tutto è valido.
  - Gestisce tre casi di errore con `try/catch`:
    - `ExpiredJwtException` → logga l'istante di scadenza (`e.getClaims().getExpiration()`) e rilancia `InvalidTokenException` con messaggio che include la data di scadenza e invito a fare nuovamente login.
    - `SignatureException` → logga che la firma non è valida e rilancia `InvalidTokenException` con messaggio "Token's signature is not valid".
    - `Exception` (catch generico) → logga "Token is not valid" e rilancia `InvalidTokenException` generica. Questo cattura qualunque altro problema (token malformato, algoritmo non atteso, claim non parsabili, ecc.).
  - In tutti i casi di errore l'eccezione originale non viene propagata al chiamante: viene sempre "tradotta" in `InvalidTokenException` (runtime exception custom, gestita poi da `GlobalExceptionHandler` con HTTP 401).

**Implicazioni di sicurezza:**
- Algoritmo **RS256** (asimmetrico): la chiave privata (usata solo per firmare) non deve mai lasciare il server; la chiave pubblica (usata per verificare) può essere distribuita senza rischi. Questo disaccoppiamento è più sicuro di un HMAC condiviso in scenari multi-servizio, ma qui viene usato in singolo servizio.
- Nessuna gestione di refresh token/rotazione visibile in questa classe (esiste un endpoint `/api/auth/rotate` menzionato in `SecurityConfig`, ma la relativa logica è in `authuser`).
- Nessun controllo esplicito sull'`issuer` (`iss`) o sull'`audience` (`aud`) del token: la validazione si basa solo su firma e scadenza.
- Il logging del token completo in chiaro a livello INFO è un rischio di esposizione di credenziali nei log.

---

### H4: Configurazione

#### `com.pat.crewhive.security.config.CoreConfig`

**Tipo:** `@Configuration` Spring.

**Scopo:** Configurazione minimale che espone l'encoder delle password usato dall'applicazione.

**Bean definiti:**
- `PasswordEncoder passwordEncoder()` → ritorna un `BCryptPasswordEncoder` (algoritmo bcrypt, con salt automatico e cost factor di default). Usato in fase di registrazione/login per hashare e verificare le password (vedi `PasswordUtil`, `AuthService`).

---

#### `com.pat.crewhive.security.config.SecurityConfig`

**Tipo:** `@Configuration` Spring, annotata anche `@EnableMethodSecurity` (abilita le annotazioni di sicurezza a livello di metodo come `@PreAuthorize`/`@PostAuthorize`/`@Secured` nei controller/service dell'applicazione).

**Scopo:** Definisce la catena di filtri HTTP di Spring Security (`SecurityFilterChain`) e la configurazione CORS globale. È la classe che orchestra tutte le altre componenti di sicurezza (JWT filter, exception handler, ecc.).

**Dipendenze iniettate via costruttore:** `JwtService`, `UserService` (iniettato ma non utilizzato direttamente all'interno del corpo dei metodi mostrati), `RestAuthenticationEntryPoint`, `RestAccessDeniedHandler`.

**Bean definiti:**

- `SecurityFilterChain filterChain(HttpSecurity http) throws Exception`:
  - **CSRF disabilitato** (`.csrf(AbstractHttpConfigurer::disable)`) — coerente con API stateless protette da JWT.
  - **Session management STATELESS** (`SessionCreationPolicy.STATELESS`) — Spring Security non crea né usa `HttpSession`; ogni richiesta deve portare il proprio JWT.
  - **Form login e HTTP Basic disabilitati** — l'unico meccanismo di autenticazione supportato è il Bearer JWT via `JwtAuthenticationFilter`.
  - **CORS abilitato** tramite `.cors(cors -> {})`, che demanda alla configurazione del bean `CorsFilter` definito più sotto nella stessa classe.
  - **Gestione errori**: `authenticationEntryPoint(restAuthenticationEntryPoint)` (invocato per richieste non autenticate su risorse protette → 401) e `accessDeniedHandler(restAccessDeniedHandler)` (invocato per utenti autenticati ma senza permessi sufficienti → 403).
  - **Regole di autorizzazione** (`authorizeHttpRequests`):
    - `POST /api/auth/login`, `POST /api/auth/register`, `POST /api/auth/register/manager`, `POST /api/auth/rotate` → `permitAll()` (pubblici).
    - `/docs`, `/docs/**`, `/swagger-ui.html`, `/swagger-ui/**` → `permitAll()` (documentazione API pubblica).
    - Qualsiasi altra richiesta (`anyRequest()`) → `authenticated()` (richiede un JWT valido processato dal filtro).
  - **Security headers** (`.headers(...)`):
    - **Content-Security-Policy**: policy restrittiva — risorse solo dalla stessa origine (`default-src 'self'`), niente frame esterni (`frame-ancestors 'none'`), immagini da sé stesso o data URI, script solo da 'self' (mitiga XSS), stili da 'self' più inline consentito (`'unsafe-inline'` — indebolisce parzialmente la protezione CSP contro XSS via `<style>`/attributi style), font da 'self' o data URI, connessioni XHR/fetch solo verso 'self'.
    - **X-Content-Type-Options**: `nosniff`, per impedire al browser di "indovinare" il MIME type.
    - **Referrer-Policy**: `NO_REFERRER` — il browser non invia mai l'header `Referer`.
    - **HSTS**: `includeSubDomains(true)`, `maxAgeInSeconds(31536000)` (1 anno) — forza HTTPS per un anno dopo la prima visita.
    - **Permissions-Policy**: `geolocation=(), microphone=(), camera=()` — disabilita accesso a geolocalizzazione, microfono, fotocamera.
    - **Cross-Origin-Opener-Policy**: `SAME_ORIGIN` — isola il contesto di navigazione (mitigazione Spectre/side-channel).
    - **Cross-Origin-Resource-Policy**: `SAME_ORIGIN` — impedisce ad altre origini di caricare le risorse servite da questa applicazione.
  - **Aggiunta del filtro JWT**: `http.addFilterBefore(new JwtAuthenticationFilter(jwtService), UsernamePasswordAuthenticationFilter.class)` — eseguito prima che il `SecurityContext` venga valutato dalle regole di autorizzazione.
  - Ritorna `http.build()`.

- `CorsFilter corsFilter()`:
  - Costruisce un `CorsConfiguration` con: `setAllowCredentials(true)`, metodi consentiti `GET, POST, PUT, QUERY, PATCH, DELETE, OPTIONS` (nota: `QUERY` non è un metodo HTTP standard/riconosciuto universalmente — probabile refuso o feature sperimentale), header consentiti `Authorization, Content-Type, Accept`, header esposti al client `Authorization`, `maxAge` di 3600 secondi.
  - **Nota**: la configurazione `registerCorsConfiguration("/**", cfg)` **non imposta esplicitamente `setAllowedOrigins`/`setAllowedOriginPatterns`** nel codice mostrato. Con `allowCredentials(true)` senza origini esplicite, Spring CORS in genere richiede comunque una configurazione delle origini per funzionare correttamente (`"*"` è incompatibile con `allowCredentials(true)`); non essendo presente nel file letto, non è possibile stabilire con certezza quali origini siano effettivamente permesse — da verificare in fase di hardening pre-produzione.
  - Registra la configurazione per tutti i path (`/**`) e ritorna un `CorsFilter` basato su `UrlBasedCorsConfigurationSource`.

---

### H4: Filtri

#### `com.pat.crewhive.security.filter.JwtAuthenticationFilter`

**Tipo:** Filter — estende `OncePerRequestFilter` di Spring.

**Dove si inserisce nella catena:** registrato in `SecurityConfig.filterChain` con `addFilterBefore(..., UsernamePasswordAuthenticationFilter.class)`, eseguito prima del filtro standard di autenticazione basata su form. Tenta di autenticare l'utente tramite Bearer token JWT presente nell'header `Authorization`, per ogni richiesta non esclusa.

**Metodi:**

- `boolean shouldNotFilter(HttpServletRequest request)`:
  - Ritorna `true` (salta il filtro) se il metodo HTTP è `OPTIONS`.
  - Ritorna `true` per una whitelist: `/api/auth/login`, `/api/auth/register`, `/api/auth/register/manager`, `/api/auth/rotate`, tutto ciò che inizia con `/actuator/health`, `/error`, `/docs`, `/docs/*`, `/swagger-ui.html`, `/swagger-ui/*`.
  - `/api/auth/logout` **non** è escluso (commento esplicito nel codice): il filtro gira comunque su quell'endpoint e tenta di autenticare la richiesta (il logout richiede un token valido).
  - Per tutti gli altri URI ritorna `false`.

- `void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)`:
  1. Se esiste già un'`Authentication` nel `SecurityContextHolder`, il filtro passa oltre senza fare nulla.
  2. Legge l'header `Authorization`; se assente o non inizia con `"Bearer "`, prosegue nella catena senza autenticare (bloccata poi dalla regola `anyRequest().authenticated()` se l'endpoint non è pubblico).
  3. Estrae il token rimuovendo il prefisso `"Bearer "`.
  4. Chiama `jwtService.validateToken(token)`: se non valido/scaduto, lancia `InvalidTokenException`, catturata dal blocco `catch (RuntimeException e)`.
  5. Estrae il `subject`: se `null`, logga un warning e prosegue senza autenticare (senza sollevare eccezione).
  6. Altrimenti effettua il parsing di `userId`, e legge i claim `email` (tramite `.get("email").toString()` — a differenza degli altri claim non usa l'overload tipizzato, quindi se assente lancerebbe `NullPointerException` non gestita esplicitamente, catturata comunque dal catch generico), `firstName`, `lastName`, `role`, `companyId`.
  7. Costruisce un `CustomUserDetails` "leggero" tramite `CustomUserDetails.fromClaims(...)` (nessun accesso al database/JPA).
  8. Crea un `UsernamePasswordAuthenticationToken` con il `CustomUserDetails` come principal, `null` come credenziali, le authority del `CustomUserDetails`.
  9. Imposta i dettagli della richiesta web (`WebAuthenticationDetailsSource().buildDetails(request)`).
  10. Imposta l'`Authentication` nel `SecurityContextHolder`.
  11. Gestione errori: `catch (io.jsonwebtoken.JwtException e)` (in pratica difficilmente raggiungibile, dato che `JwtService.validateToken` traduce sempre in `InvalidTokenException`) e `catch (RuntimeException e)` (il ramo che effettivamente intercetta `InvalidTokenException`), entrambi puliscono il `SecurityContext` e loggano.
  12. In ogni caso il filtro invoca sempre `chain.doFilter(request, response)` alla fine, lasciando che siano poi le regole di autorizzazione e gli handler (`RestAuthenticationEntryPoint`/`RestAccessDeniedHandler`) a determinare l'esito finale se l'utente non risulta autenticato.

**Implicazioni di sicurezza:** il filtro non genera mai una risposta di errore direttamente — si limita a non popolare (o a ripulire) il `SecurityContext`, delegando il rifiuto della richiesta al meccanismo standard di Spring Security.

---

### H4: Eccezioni

#### `com.pat.crewhive.security.exception.custom.InvalidTokenException`

**Tipo:** Eccezione custom, estende `RuntimeException`.

**Scopo:** Errore di validazione del token JWT (firma non valida, token scaduto, token malformato). Sollevata da `JwtService.validateToken`. Costruttore unico `InvalidTokenException(String message)`. Gestita da `GlobalExceptionHandler` con HTTP 401.

#### `com.pat.crewhive.security.exception.custom.JwtAuthenticationException`

**Tipo:** Eccezione custom, estende `org.springframework.security.core.AuthenticationException`.

**Scopo:** Errore di autenticazione JWT generico. Costruttore unico `JwtAuthenticationException(String message)`. Gestita da `GlobalExceptionHandler` con HTTP 401. Non risulta invocata direttamente nei file analizzati (`JwtAuthenticationFilter` usa `InvalidTokenException` tramite `JwtService`).

#### `com.pat.crewhive.security.exception.custom.ResourceAlreadyExistsException`

**Tipo:** Eccezione custom, estende `RuntimeException`.

**Scopo:** Segnala che una risorsa con lo stesso identificativo esiste già. Corrisponde a **409 Conflict**. Costruttore unico `ResourceAlreadyExistsException(String message)`. Gestita da `GlobalExceptionHandler` con HTTP 409.

#### `com.pat.crewhive.security.exception.custom.ResourceNotFoundException`

**Tipo:** Eccezione custom, estende `RuntimeException`.

**Scopo:** Segnala che una risorsa richiesta non esiste. Corrisponde a **404 Not Found**. Costruttore unico `ResourceNotFoundException(String message)`. Gestita da `GlobalExceptionHandler` con HTTP 404.

#### `com.pat.crewhive.security.exception.handler.GlobalExceptionHandler`

**Tipo:** ExceptionHandler globale — `@RestControllerAdvice`, applicato a tutti i `@RestController` dell'applicazione.

**Scopo:** Centralizza la traduzione di tutte le eccezioni applicative in risposte HTTP strutturate secondo `ProblemDetail` (RFC 7807, "application/problem+json").

**Metodi privati di supporto:**
- `ProblemDetail base(HttpStatus status, String title, String detail, String errorCode)`: crea un `ProblemDetail`, imposta `title`, `type = about:blank` (placeholder), aggiunge `timestamp` ed `errorCode`.
- `ProblemDetail withPath(ProblemDetail pd, String path)`: aggiunge la proprietà `path`. **Nota**: non risulta mai invocato da nessuno degli `@ExceptionHandler` della classe — il campo `path` non viene mai popolato nelle risposte di errore prodotte da questo handler (a differenza di `RestAccessDeniedHandler`/`RestAuthenticationEntryPoint`, che lo impostano manualmente). Probabile inconsistenza/codice non completato.

**Handler delle eccezioni** (tutti ritornano `ProblemDetail`):

| Eccezione gestita                         | HTTP Status | errorCode                  | Note                                                                                                                     |
|-------------------------------------------|-------------|----------------------------|--------------------------------------------------------------------------------------------------------------------------|
| `MethodArgumentNotValidException`         | 400         | `VAL_400`                  | Raccoglie tutti gli errori di campo (Bean Validation) in una mappa `campo → messaggio`, aggiunta come proprietà `errors` |
| `AuthorizationDeniedException`            | 403         | `AUTH_403_DENIED`          | Autorizzazione negata (tipicamente da `@PreAuthorize`)                                                                   |
| `ObjectOptimisticLockingFailureException` | 409         | `DATA_409_OPTIMISTIC_LOCK` | Conflitto di versione (optimistic locking JPA)                                                                           |
| `HttpMessageNotReadableException`         | 400         | `JSON_400`                 | Payload JSON malformato/non leggibile                                                                                    |
| `ResourceNotFoundException`               | 404         | `RES_404`                  | Risorsa applicativa non trovata                                                                                          |
| `ResourceAlreadyExistsException`          | 409         | `RES_409`                  | Risorsa già esistente                                                                                                    |
| `DataIntegrityViolationException`         | 409         | `DATA_409_INTEGRITY`       | Violazione di vincoli di integrità sul DB                                                                                |
| `BadCredentialsException`                 | 401         | `AUTH_401_BAD_CREDENTIALS` | Credenziali di login errate                                                                                              |
| `InvalidTokenException`                   | 401         | `AUTH_401_INVALID_TOKEN`   | Token JWT non valido/scaduto                                                                                             |
| `JwtAuthenticationException`              | 401         | `AUTH_401_JWT`             | Errore di autenticazione JWT generico                                                                                    |
| `IllegalArgumentException`                | 400         | `GEN_400_ILLARG`           | Argomenti non validi (usa `ex.getMessage()` come detail, quindi può esporre il messaggio dell'eccezione al client)       |
| `IllegalStateException`                   | 500         | `GEN_500_ILLSTATE`         | Stato applicativo illegale, messaggio generico al client                                                                 |
| `RuntimeException`                        | 500         | `GEN_500_RUNTIME`          | Fallback per ogni altra `RuntimeException` non gestita specificamente                                                    |
| `Exception`                               | 500         | `GEN_500`                  | Fallback finale per qualunque eccezione non gestita da nessun altro handler                                              |

Ogni handler logga l'evento con livello appropriato (`log.warn` per errori "attesi"/client, `log.error` per errori server-side/imprevisti, `log.info` per eventi di business).

**Implicazioni di sicurezza:** per gli errori 500 generici il messaggio restituito al client è sempre un testo fisso generico ("An unexpected error occurred"), evitando di esporre stack trace; l'eccezione completa viene loggata server-side. Per `IllegalArgumentException` invece il messaggio effettivo (`ex.getMessage()`) viene restituito al client: da verificare caso per caso per evitare di esporre dettagli implementativi sensibili.

#### `com.pat.crewhive.security.exception.handler.RestAccessDeniedHandler`

**Tipo:** `AccessDeniedHandler` (Spring Security), `@Component`, collegato in `SecurityConfig`.

**Scopo:** Gestisce il caso in cui un utente **autenticato** tenta di accedere a una risorsa per cui non ha i permessi (403 Forbidden), producendo una risposta JSON coerente in stile `ProblemDetail`.

**Dipendenza:** `ObjectMapper` (da `tools.jackson.databind`, Jackson 3.x) iniettato via costruttore.

**Metodo:**
- `void handle(HttpServletRequest request, HttpServletResponse response, AccessDeniedException ex)`: costruisce un `ProblemDetail` con status 403, title "Forbidden", `timestamp`, `path` (`request.getRequestURI()`, effettivamente popolato qui), `errorCode = "AUTH_403"`. Imposta status 403, content-type `application/json; charset=UTF-8`, e scrive il JSON sul writer della risposta.

#### `com.pat.crewhive.security.exception.handler.RestAuthenticationEntryPoint`

**Tipo:** `AuthenticationEntryPoint` (Spring Security), `@Component`, collegato in `SecurityConfig`.

**Scopo:** Gestisce il caso in cui un utente **non autenticato** tenta di accedere a una risorsa protetta (401 Unauthorized), producendo una risposta JSON in stile `ProblemDetail`. È il punto finale che l'utente vede effettivamente come risposta di errore quando `JwtAuthenticationFilter` non riesce a popolare il `SecurityContext` (token assente, non valido o scaduto).

**Dipendenza:** `ObjectMapper` iniettato via costruttore.

**Metodo:**
- `void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException ex)`: costruisce un `ProblemDetail` con status 401, title "Unauthorized", `timestamp`, `path`, `errorCode = "AUTH_401"`. Imposta status 401, content-type `application/json; charset=UTF-8`, scrive il JSON sul writer della risposta.

---

### H4: Gestione Chiavi JWT

#### `com.pat.crewhive.security.key.JwtKeyConfig`

**Tipo:** `@Configuration` Spring.

**Scopo:** Espone come bean Spring le chiavi RSA (`PrivateKey`, `PublicKey`) usate da `JwtService`, delegando il parsing effettivo del formato PEM a `PemUtils`.

**Bean definiti:**
- `PrivateKey jwtPrivateKey() throws Exception` → richiama `pemUtils.loadPrivateKey()`.
- `PublicKey jwtPublicKey() throws Exception` → richiama `pemUtils.loadPublicKey()`.

Entrambi i bean vengono iniettati nel costruttore di `JwtService`: proprietà di configurazione (`JwtKeyProperties`) → parsing PEM (`PemUtils`) → bean chiave (`JwtKeyConfig`) → uso nella firma/verifica JWT (`JwtService`).

#### `com.pat.crewhive.security.key.JwtKeyProperties`

**Tipo:** Classe di binding delle proprietà di configurazione — `@ConfigurationProperties(prefix = "jwt")`, anche `@Configuration`, con `@Getter`/`@Setter` (Lombok).

**Scopo:** Mappa le proprietà con prefisso `jwt.*` su due campi stringa: `privateKey` e `publicKey`, contenenti il testo PEM completo delle chiavi RSA.

**Implicazioni di sicurezza:** la chiave privata RSA (elemento più critico del sistema — chiunque la ottenga può forgiare token validi per qualsiasi utente/ruolo) viene caricata da configurazione esterna, non hardcoded nel codice sorgente. Non essendo presenti file di configurazione (`application.yml`) tra i file analizzati, non è verificabile come/dove sia effettivamente valorizzata (variabile d'ambiente, secret manager, ecc.) — questo è un punto chiave da presidiare nel passaggio a produzione (vedi discussione su gestione segreti/VPS).

#### `com.pat.crewhive.security.key.PemUtils`

**Tipo:** Utility Spring (`@Component`) — parsa il testo PEM delle chiavi RSA in oggetti `java.security.PrivateKey`/`PublicKey`.

**Dipendenza:** `JwtKeyProperties` iniettata via costruttore.

**Metodi pubblici:**

- `PrivateKey loadPrivateKey() throws Exception`: rimuove header/footer `-----BEGIN/END PRIVATE KEY-----` e spazi bianchi dalla stringa PEM, decodifica Base64, costruisce una `PKCS8EncodedKeySpec`, genera la `PrivateKey` con `KeyFactory.getInstance("RSA")`.
- `PublicKey loadPublicKey() throws Exception`: analogo, rimuove header/footer `-----BEGIN/END PUBLIC KEY-----`, decodifica Base64, costruisce una `X509EncodedKeySpec`, genera la `PublicKey` con `KeyFactory.getInstance("RSA")`.

**Implicazioni di sicurezza:** entrambi i metodi si affidano ciecamente al formato/algoritmo dichiarato ("RSA"); se le proprietà fossero assenti/malformate, l'errore si manifesterebbe già in fase di avvio dell'applicazione (i bean di `JwtKeyConfig` sono creati eagerly), impedendo il bootstrap del contesto Spring — comportamento fail-fast desiderabile per un sistema di sicurezza.

---

### H4: Sanificazione Input

#### `com.pat.crewhive.security.sanitizer.HtmlSanitizer`

**Tipo:** Component Spring (`@Component`) — sanificazione attiva (rimuove contenuto pericoloso).

**Scopo:** Ripulisce stringhe da qualunque markup HTML/script, per ridurre il rischio di XSS stored.

**Campo:** `private static final Safelist NONE = Safelist.none()` — nessun tag/attributo HTML consentito.

**Metodo pubblico:**
- `String stripAll(String input)`: se `input` è `null` ritorna `null`; altrimenti `Jsoup.clean(input, NONE)`, che rimuove ogni tag HTML/script. Presente un commento `//todo` per un futuro metodo `basic(String input)` con Safelist meno restrittiva, non ancora implementato.

**Implicazioni di sicurezza:** `Safelist.none()` è la configurazione più sicura possibile. A differenza di `NoHtmlValidator`, questa classe **modifica** l'input invece di rifiutarlo.

#### `com.pat.crewhive.security.sanitizer.NoHtmlValidator`

**Tipo:** Validator Bean Validation — implementa `ConstraintValidator<NoHtml, String>`, motore di `@NoHtml`.

**Scopo:** Verifica (senza modificare) che una stringa non contenga markup HTML/JS.

**Metodo pubblico:**
- `boolean isValid(String value, ConstraintValidatorContext ctx)`: se `value` è `null` ritorna `true`. Altrimenti confronta `Jsoup.clean(value, Safelist.none()).equals(value)`: se coincide, l'input non conteneva markup (valido); altrimenti fallisce (messaggio di default "HTML/JS non consentito").

**Implicazioni di sicurezza:** controllo basato su parsing HTML reale (non regex), robusto contro tecniche di evasione. A differenza di `HtmlSanitizer`, rigetta esplicitamente l'input (HTTP 400) invece di modificarlo silenziosamente.

#### `com.pat.crewhive.security.sanitizer.annotation.NoHtml`

**Tipo:** Annotazione custom di Bean Validation — `@Constraint(validatedBy = NoHtmlValidator.class)`.

**Scopo:** Vincolo applicabile a campi/parametri (`FIELD`, `PARAMETER`), `RUNTIME` retention, `@Documented`.

**Elementi:** `message() default "HTML/JS non consentito"`, `groups()`, `payload()` (standard Bean Validation).

Usata estensivamente su campi testuali dei DTO in tutta l'applicazione per impedire markup HTML/script.

---

### H4: Utility

#### `com.pat.crewhive.security.util.UserUtils`

**Tipo:** Classe utility statica (`final`, costruttore privato) — accesso ai dati dell'utente autenticato dal `SecurityContextHolder`.

**Metodi pubblici (tutti statici):**

- `Authentication getAuthentication()`: ritorna `SecurityContextHolder.getContext().getAuthentication()`.
- `boolean isAuthenticated()`: `true` solo se `Authentication` non è `null`, `isAuthenticated()` è `true`, e il principal non è la stringa `"anonymousUser"`.
- `CustomUserDetails getCustomUserDetails()`: ritorna il principal castato a `CustomUserDetails` se disponibile e autenticato, altrimenti `null`.
- `UserDTO getCurrentUser()`: costruisce un `UserDTO(email, firstName, lastName, role, companyId)` dall'utente corrente, o `null`.
- `Long getCurrentUserId()`, `String getCurrentUsername()`, `String getCurrentUserRole()`: getter di comodo, `null` se non autenticato.
- `boolean hasRole(String role)`: normalizza `role` prefissando `"ROLE_"` se assente, poi verifica se una delle `authorities` dell'utente corrente coincide esattamente. **Nota**: lancia `NullPointerException` se `role` è `null` (nessun controllo).

**Implicazioni di sicurezza:** helper puramente di lettura, non esegue controlli di autorizzazione attivi — è compito del chiamante usare `hasRole`/`isAuthenticated` per negare l'accesso. Confronto case-sensitive: eventuali incongruenze di case tra il claim `role` del JWT e il parametro passato a `hasRole` causerebbero falsi negativi silenziosi.

---

## Feature: Autenticazione Utente (authuser)

### `com.pat.crewhive.authuser.AuthRequestDTO`
**Tipo:** DTO (classe) usato come corpo della richiesta di login.

**Scopo:** Trasporta le credenziali (email e password) inviate dal client all'endpoint di login.

**Campi:**
- `email` (`String`): `@NotBlank` ("Email cannot be blank"), `@NoHtml`, `@Size(min=3, max=32)`.
- `password` (`String`): `@NotBlank` ("Password cannot be blank"), `@NoHtml`. Nessun vincolo di lunghezza (la robustezza è verificata solo in registrazione).

Lombok: `@Getter`, `@Setter`, `@AllArgsConstructor`, `@NoArgsConstructor`.

---

### `com.pat.crewhive.authuser.AuthResponseDTO`
**Tipo:** DTO (classe) restituito dagli endpoint di login e rotate.

**Scopo:** Incapsula la coppia di token restituita al client dopo autenticazione o rotazione.

**Campi:**
- `accessToken` (`String`): `@NotBlank` — il JWT.
- `refreshToken` (`String`): `@NotBlank` — il token di refresh (stringa UUID).

Lombok: `@Getter`, `@Setter`, `@AllArgsConstructor`, `@NoArgsConstructor`.

---

### `com.pat.crewhive.authuser.AuthService`
**Tipo:** Service (`@Service`, `@Slf4j`).

**Scopo:** Logica di business per login, registrazione, rotazione del refresh token e logout. Orchestra `UserService`, `UserRepository`, `JwtService`, `RefreshTokenService`, `RoleService`, `PasswordUtil`, `EmailUtil`, `StringUtils`.

**Metodi pubblici:**

- **`AuthResponseDTO login(AuthRequestDTO request)`** — `@Transactional`.
  1. Normalizza l'email (`stringUtils.normalizeString`, trim + lowercase).
  2. Recupera l'utente tramite `userService.getUserByEmail`.
  3. Verifica la password con `passwordUtil.NotMatches`; se non corrisponde, lancia `BadCredentialsException("Invalid credentials")`.
  4. Se presente un refresh token esistente dell'utente, lo invalida — ogni login invalida la sessione di refresh precedente.
  5. Determina l'id della company dell'utente (può essere `null`).
  6. Genera un nuovo JWT e un nuovo refresh token.
  7. Ritorna `AuthResponseDTO` con entrambi i token.

- **`void register(RegistrationDTO request)`** — `@Transactional`.
  1. Normalizza l'email.
  2. Valida il formato con `emailUtil.isValidEmail`; se non valido, `BadCredentialsException("Invalid email format")`.
  3. Verifica che l'email non sia già registrata (`ResourceAlreadyExistsException` altrimenti).
  4. Verifica la robustezza della password (`passwordUtil.isStrong`); se debole, `BadCredentialsException("Weak password provided")`.
  5. Codifica la password.
  6. Crea un nuovo `User`.
  7. Ottiene/crea il ruolo globale "USER" (`roleService.getOrCreateGlobalRoleUser()`) e lo assegna.
  8. Salva l'utente.
  9. Non ritorna nulla (il Javadoc dichiara erroneamente un valore di ritorno — il metodo è `void`, discrepanza tra commento e implementazione).

- **`AuthResponseDTO rotate_token(String token)`** — `@Transactional`.
  1. Se `token` è nullo/vuoto, `InvalidTokenException`.
  2. Valida che sia un UUID sintatticamente valido, altrimenti `InvalidTokenException`.
  3. Recupera il `RefreshToken` con utente e ruolo caricati eagerly.
  4. Se scaduto, `InvalidTokenException`.
  5. Verifica che il possessore non sia nullo, altrimenti `InvalidTokenException`.
  6. Estrae dati dell'owner (userId, email normalizzata, nome, cognome, ruolo, company/companyId).
  7. Genera un nuovo access token.
  8. Ruota il refresh token esistente (nuovo valore, nuova scadenza, stesso record).
  9. Ritorna `AuthResponseDTO` aggiornato.

- **`void logout(LogoutDTO request)`** — `@Transactional`.
  1. Se `refreshToken` è nullo/vuoto, `InvalidTokenException("Refresh Token is missing")`.
  2. Recupera il `RefreshToken`.
  3. Se `null` o scaduto, `InvalidTokenException("Refresh Token expired or missing")`.
  4. Recupera il proprietario; se `null` o `userId` non coincide con quello richiesto, `InvalidTokenException("Refresh Token does not belong to user")` — impedisce che un utente invalidi il token di un altro.
  5. Invalida (cancella) il refresh token.
  6. Logga l'avvenuto logout.

---

### `com.pat.crewhive.authuser.AuthUserController`
**Tipo:** REST Controller (`@RestController`, `@Slf4j`), `/api/auth`, implementa `AuthUserControllerInterface`.

**Scopo:** Espone gli endpoint HTTP di autenticazione, delegando a `AuthService`.

**Endpoint:**

- **`POST /api/auth/rotate`** — `rotate(@AuthenticationPrincipal CustomUserDetails cud, @RequestBody @Valid RotateRequestDTO request)`. Richiede principal autenticato. Chiama `authService.rotate_token(...)`. `200 OK` con `AuthResponseDTO`.
- **`POST /api/auth/register`** — `register(@RequestBody @Valid RegistrationDTO rDTO)`. Endpoint pubblico. `201 CREATED` senza corpo.
- **`POST /api/auth/login`** — `login(@RequestBody @Valid AuthRequestDTO request)`. Endpoint pubblico. `200 OK` con `AuthResponseDTO`.

---

### `com.pat.crewhive.authuser.AuthUserControllerInterface`
**Tipo:** Interfaccia per la documentazione OpenAPI/Swagger (`@Tag(name = "Authentication")`) di `AuthUserController`. Nessuna logica.

Nota: la documentazione Swagger per `register` indica 200 come codice di successo, ma l'implementazione reale ritorna `201 CREATED` — piccola incoerenza tra doc e comportamento reale.

---

### `com.pat.crewhive.authuser.EmailUtil`
**Tipo:** Utility component (`@Component`).

**Metodi pubblici:**
- **`boolean isValidEmail(String email)`**: applica la regex `^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,}$`. Non gestisce `email == null` esplicitamente (lancerebbe `NullPointerException`).

---

### `com.pat.crewhive.authuser.RefreshToken`
**Tipo:** Entity JPA, tabella `refresh_token`.

**Campi:**
- `refreshTokenId` (Long, PK, IDENTITY).
- `token` (String) — valore del refresh token (UUID stringa).
- `user` (`User`, `@ManyToOne(LAZY, optional=false)`, `@JoinColumn(user_id, unique=true)`) — il vincolo `unique` implica al massimo un refresh token attivo per utente.
- `expirationDate` (`LocalDate`).

Indici: `idx_refreshtoken` su id, `idx_refreshtoken_user_id` su `user_id`.

---

### `com.pat.crewhive.authuser.RefreshTokenRepository`
**Tipo:** Repository Spring Data JPA.

**Metodi:**
- `void deleteByUser(User user)`.
- `Optional<RefreshToken> findByToken(String token)`.
- `Optional<RefreshToken> findByUser(User user)`.
- `Optional<RefreshToken> findByTokenWithUserAndRole(String token)` — JPQL con `join fetch rt.user u left join fetch u.role r`, evita N+1.

---

### `com.pat.crewhive.authuser.RefreshTokenService`
**Tipo:** Service (`@Service`, `@Slf4j`).

**Metodi pubblici:**

- **`String generateRefreshToken(User user)`** — `@Transactional`. Elimina token esistenti per l'utente, crea uno nuovo con `token = UUID.randomUUID().toString()` e `expirationDate = LocalDate.now().plusDays(15)`, salva, ritorna il token.
- **`RefreshToken getRefreshToken(String token)`** — cerca per token; `ResourceNotFoundException` se assente.
- **`RefreshToken getRefreshTokenByUser(User user)`** — ritorna `null` se assente o scaduto (non lancia eccezione).
- **`RefreshToken getRefreshTokenByTokenWithUserAndRole(String token)`** — query con utente+ruolo caricati eagerly; `ResourceNotFoundException` se assente.
- **`boolean isExpired(RefreshToken rt)`** — `IllegalArgumentException` se `rt == null`; altrimenti confronta `expirationDate` con oggi.
- **`String getOrIssueRefreshToken(User user)`** — riusa il token valido esistente o ne genera uno nuovo se assente/scaduto.
- **`String rotateRefreshToken(RefreshToken rt)`** — aggiorna in-place lo stesso record con nuovo token e nuova scadenza (+15 giorni).
- **`User getOwner(RefreshToken rt)`** — `IllegalArgumentException` se `rt`/`rt.getUser()` nulli.
- **`void invalidateRefreshToken(RefreshToken rt)`** — `ResourceNotFoundException` se `rt == null` (nota: tipo di eccezione diverso da quello usato in `isExpired`/`getOwner`/`rotateRefreshToken`, che usano `IllegalArgumentException` — incoerenza tra metodi simili). Altrimenti elimina il record.
- **`void deleteTokenByUser(User user)`** — elimina (se presente) il refresh token dell'utente.

---

### `com.pat.crewhive.authuser.RegistrationDTO`
**Tipo:** DTO (classe) per la registrazione.

**Campi e validazioni:**
- `email`: `@NotBlank`, `@Email`, `@Size(min=5, max=32)`, `@NoHtml`.
- `firstName`, `lastName`: `@NotBlank`, `@NoHtml`, `@Size(min=3, max=32)`.
- `password`: `@NotBlank`, `@Size(min=8, max=32)`, `@NoHtml`.

---

### `com.pat.crewhive.authuser.RotateRequestDTO`
**Tipo:** DTO (classe) per la rotazione token.

**Campi:**
- `refreshToken` (`String`): `@NotBlank`, `@NoHtml`.

---

## Feature: Gestione Utenti (user)

### `com.pat.crewhive.user.User`
**Tipo:** Entity JPA, tabella `users`, indici su `username` (nota: non esiste un campo `username` nell'entity — l'indice sembra riferirsi a una colonna non più presente/gestita) e `company_id`.

**Scopo:** Anagrafica, credenziali, appartenenza azienda, ruolo, parametri contrattuali/orari (ferie, permessi, straordinari), relazioni con turni ed eventi personali.

**Campi principali:**
- `userId` (Long, PK, IDENTITY, setter disabilitato).
- `email` (String, `unique = true`).
- `firstName`, `lastName` (String, not null).
- `password` (String, not null; `@JsonProperty(access = WRITE_ONLY)` — mai serializzato in output).
- `company` (`@ManyToOne(LAZY)`, `@JsonIgnore`).
- `isWorking` (boolean).
- `personalEvents` (`Set<EventUsers>`, `@OneToMany(mappedBy="user", cascade=ALL, orphanRemoval=true, LAZY)`, `@JsonManagedReference`).
- `role` (`UserRole`, `@OneToOne(mappedBy="user", cascade=ALL, orphanRemoval=true)`).
- `contractType` (`ContractType`, `@Enumerated(STRING)`).
- `workableHoursPerWeek` (int).
- `overtimeHours`, `vacationDaysAccumulated`, `vacationDaysTaken`, `leaveDaysAccumulated`, `leaveDaysTaken` (BigDecimal, not null).
- `shiftWorked` (`Set<ShiftWorked>`, `@OneToMany(mappedBy="user", cascade=ALL, orphanRemoval=true, LAZY)`).
- `shiftUsers` (`Set<ShiftUser>`, `@OneToMany(mappedBy="user", cascade=ALL, orphanRemoval=true, LAZY)`, `@JsonManagedReference`).

**Annotazioni Jackson:** `@JsonIdentityInfo(generator=PropertyGenerator.class, property="userId")` per evitare cicli di serializzazione (es. `User` → `ShiftUser` → `User`).

**Costruttore custom:** `User(String email, String firstName, String lastName, String password)` — imposta `isWorking=false`, `workableHoursPerWeek=0`, tutti i contatori a `BigDecimal.ZERO`. Usato in fase di registrazione.

---

### `com.pat.crewhive.user.ContractType`
**Tipo:** Enum — `FULL_TIME`, `PART_TIME_HORIZONTAL`, `PART_TIME_VERTICAL`, ciascuno con `getLabel()`. Usato per determinare l'accredito mensile automatico di ferie/permessi (cron).

---

### `com.pat.crewhive.user.UserDTO`
**Tipo:** DTO.

**Campi e validazioni:**
- `email`: `@NotBlank`, `@NoHtml`, `@Size(min=1, max=6)` — **nota**: vincolo di lunghezza massima di 6 caratteri per un'email appare anomalo (probabile errore di configurazione), ma è quanto scritto nel codice.
- `firstName`, `lastName`, `role`: `@NotBlank`, `@NoHtml`, `@Size(min=3, max=32)`.
- `companyId` (Long): nessuna validazione esplicita.

---

### `com.pat.crewhive.user.UserWithTimeParamsDTO`
**Tipo:** DTO di output — profilo utente completo (`GET /api/user/me`).

**Campi:** `userId`, `firstName`, `lastName`, `email`, `companyName` (o `null`), `contractType`, `workableHoursPerWeek`, `overtimeHours`, `vacationDaysAccumulated`, `vacationDaysTaken`, `leaveDaysAccumulated`, `leaveDaysTaken`.

---

### `com.pat.crewhive.user.LogoutDTO`
**Tipo:** DTO input. **Campi:** `userId` (`@NotNull`), `refreshToken` (`@NotBlank`, `@NoHtml`).

### `com.pat.crewhive.user.UpdatePasswordDTO`
**Tipo:** DTO input. **Campi:** `oldPassword`, `newPassword` (entrambi `@NotBlank`, `@NoHtml`).

### `com.pat.crewhive.user.RemoveUserFromCompanyOutputDTO`
**Tipo:** DTO output. **Campi:** `accessToken` (String). Non risulta utilizzato nei file letti (il metodo `leaveCompany` restituisce invece `AuthResponseDTO`).

---

### `com.pat.crewhive.user.UserRepository`
**Tipo:** Repository Spring Data JPA.

**Metodi:**
- `findById(Long id)`: `@EntityGraph(attributePaths={"role","role.role","shiftUsers","shiftUsers.shift"})`.
- `findAllByCompany_CompanyId(Long companyId)`: `List<User>`.
- `findByEmail(String email)`: `@EntityGraph(attributePaths={"role","role.role"})`.
- `findByUsername(String username)`: stesso `@EntityGraph` esteso di `findById`. **Nota**: l'entity `User` non possiede un campo `username` — questo metodo non può funzionare come query derivata automatica su un campo inesistente; probabile residuo di refactoring, da verificare.
- `findAllByIds(Set<Long> ids)`: `@Query("select u from User u where u.userId in :ids")`, `@EntityGraph(attributePaths={"role","role.role"})`.
- `existsByEmail(String email)`, `existsByUsername(String username)` (stessa nota di `findByUsername`).
- `accrueMonthlyVacationDays()`: `int` — query nativa `@Modifying` che aggiunge **2.17** giorni (FULL_TIME/PART_TIME_HORIZONTAL) o **1.3** giorni (PART_TIME_VERTICAL) a `vacation_days_accumulated`, per utenti con `contract_type IS NOT NULL`.
- `accrueMonthlyLeaveDays()`: `int` — analoga, aggiunge **1.5**/**0.9** giorni a `leave_days_accumulated`.

---

### `com.pat.crewhive.user.UserService`
**Tipo:** Service.

**Metodi pubblici:**

- `updateUser(User user)`: `void`, `@Transactional`. Salva l'entity.
- `getUserById(Long id)`: `User`, readOnly. `ResourceNotFoundException` se assente.
- `getUsersByIds(Set<Long> ids)`: `List<User>`, readOnly. Ritorna lista vuota se `ids` nullo/vuoto; se il numero di risultati differisce dal richiesto, `ResourceNotFoundException` con gli ID mancanti.
- `getUserByEmail(String email)`: `User`, readOnly. Normalizza l'email; `ResourceNotFoundException` se assente.
- `getUserWithTimeParamsByUsername(Long userId)`: `UserWithTimeParamsDTO`, readOnly. Nonostante il nome, il parametro è uno `userId`. Costruisce il DTO completo con `companyName` (o `null`).
- `getAllUsersInCompany(Long companyId)`: `List<User>`, readOnly.
- `updatePassword(String newPassword, String oldPassword, String email)`: `void`, `@Transactional`. Verifica robustezza nuova password (`BadCredentialsException("Invalid password")` altrimenti) e corrispondenza vecchia password (`BadCredentialsException("Old password does not match")` altrimenti); poi aggiorna.
- `updateUserTimeParams(UpdateUserWorkInfoDTO dto, Long companyId)`: `void`, `@Transactional`. Verifica che l'utente target appartenga alla company specificata (`ResourceNotFoundException` altrimenti); aggiorna tutti i campi contrattuali/orari.
- `leaveCompany(Long userId)`: `AuthResponseDTO`, `@Transactional`. Se `user.getCompany()` è `null`, `ResourceNotFoundException("User has no company")` (nota: il Javadoc dichiara `ResourceAlreadyExistsException`, ma il codice lancia `ResourceNotFoundException` — incoerenza tra commento e implementazione). Rimuove l'utente dalla company, elimina le associazioni turno-utente, salva, genera un nuovo JWT con ruolo resettato a `"ROLE_USER"` e `company/companyId = null`, e un refresh token.
- `deleteAccount(Long userId)`: `void`, `@Transactional`. Elimina il refresh token e l'utente (cascata `ALL`+`orphanRemoval` elimina anche `personalEvents`, `shiftWorked`, `shiftUsers`, `role`).

---

### `com.pat.crewhive.user.UserController`
**Tipo:** Controller REST, `/api/user`, implementa `UserControllerInterface`.

**Endpoint:**
1. `GET /api/user/me` — `getUser(@AuthenticationPrincipal CustomUserDetails cud)`: `200 OK` con `UserWithTimeParamsDTO`.
2. `POST /api/user/logout` — `logout(@RequestBody @Valid LogoutDTO request)`: `200 OK` senza corpo.
3. `PATCH /api/user/update-password` — `updatePassword(cud, @RequestBody @Valid UpdatePasswordDTO)`: usa l'email dell'utente autenticato (non quella del DTO, che non ne contiene una); `200 OK` senza corpo.
4. `DELETE /api/user/leave-company` — `leaveCompany(cud)`: `200 OK` con `AuthResponseDTO` (nuovo token, dato che il ruolo cambia).
5. `DELETE /api/user/delete-account` — `deleteAccount(cud)`: `200 OK` senza corpo (elimina definitivamente l'account).

Tutti gli endpoint richiedono autenticazione (Bearer JWT).

---

### `com.pat.crewhive.user.UserControllerInterface`
**Tipo:** Interfaccia di documentazione Swagger (`@Tag(name = "User Management")`). Nessuna logica.

---

### `com.pat.crewhive.user.MonthlyVacationCron`
**Tipo:** Cron/Scheduled (`@Component`). `@Scheduled(cron = "0 0 2 1 * *", zone = "Europe/Rome")` — 02:00 del primo giorno di ogni mese.

**Logica:** invoca `userRepository.accrueMonthlyVacationDays()` (+2.17/+1.3 giorni), logga il numero di utenti aggiornati.

### `com.pat.crewhive.user.MonthlyLeaveDaysCron`
**Tipo:** Cron/Scheduled (`@Component`). Stessa frequenza (`"0 0 2 1 * *"`, `Europe/Rome`).

**Logica:** invoca `userRepository.accrueMonthlyLeaveDays()` (+1.5/+0.9 giorni). **Nota**: il messaggio di log riporta erroneamente "MonthlyVacationCron eseguito" invece di "MonthlyLeaveDaysCron" — refuso di copy-paste, non impatta la logica.

---

## Feature: Utility Comuni (common)

### `com.pat.crewhive.common.ContractJSON`
**Tipo:** DTO/classe di supporto (non entity JPA), presumibilmente serializzata come colonna JSON.

**Campi:** `startDate`, `endDate` (`LocalDate`, formato `yyyy-MM-dd`), `hoursPerWeek` (int), `indefinite` (boolean).

**Costruttore:** `ContractJSON(LocalDate startDate, LocalDate endDate, int hoursPerWeek, boolean indefinite)` — se `indefinite == true`, forza `endDate = null` indipendentemente dal valore passato.

---

### `com.pat.crewhive.common.DateUtils`
**Tipo:** Utility component (`@Component`, `@Slf4j`).

**Scopo:** Calcola intervalli di date (inizio/fine) per un `Period` predefinito, relativi a "oggi".

**Metodi pubblici:**
- **`LocalDate getStartDateForPeriod(Period period)`**: `DAY`→oggi; `WEEK`→lunedì corrente; `MONTH`→1° del mese corrente; `TRIMESTER`→1° del mese precedente; `SEMESTER`→1° di 2 mesi prima; `YEAR`→1° di 5 mesi prima.
- **`LocalDate getEndDateForPeriod(Period period)`**: `DAY`→oggi; `WEEK`→domenica corrente; `MONTH`→ultimo giorno mese corrente; `TRIMESTER`→ultimo giorno mese successivo; `SEMESTER`→ultimo giorno di 2 mesi dopo; `YEAR`→ultimo giorno di 5 mesi dopo.

**Osservazione importante:** i nomi `TRIMESTER`, `SEMESTER`, `YEAR` sono fuorvianti rispetto alla logica reale: combinando start+end, `TRIMESTER` copre ~3 mesi (mese-1 → mese+1), `SEMESTER` ~5 mesi (mese-2 → mese+2), `YEAR` ~11 mesi (mese-5 → mese+5), sempre centrati sul mese corrente — non corrispondono a un anno/semestre calendariale standard.

---

### `com.pat.crewhive.common.PasswordUtil`
**Tipo:** Utility component (`@Component`).

**Costanti:** `MIN_LEN = 8`, `MAX_LEN = 20` (più restrittivo del `@Size(max=32)` di `RegistrationDTO.password` — una password tra 21 e 32 caratteri passerebbe la validazione del DTO ma verrebbe rifiutata da `isStrong`).

**Metodi pubblici:**
- **`boolean isStrong(String password)`**: `false` se `null` o lunghezza (code point) fuori `[8,20]`; altrimenti verifica presenza di maiuscola, minuscola, cifra, simbolo/punteggiatura (Unicode-safe, gestisce surrogate pairs); `true` solo se tutte e quattro presenti.
- **`private boolean isSymbolOrPunctuation(int cp)`**: controlla se il tipo Unicode del code point rientra tra le categorie di punteggiatura/simbolo.
- **`String encodePassword(String raw)`**: `NullPointerException` se `raw == null`; altrimenti delega al `PasswordEncoder` (BCrypt).
- **`boolean NotMatches(String rawPassword, String encodedPassword)`**: negazione di `passwordEncoder.matches`. Nota di stile: nome che inizia con maiuscola, non conforme alla naming convention Java.

---

### `com.pat.crewhive.common.Period`
**Tipo:** Enum — `DAY`, `WEEK`, `MONTH`, `TRIMESTER`, `SEMESTER`, `YEAR`.

---

### `com.pat.crewhive.common.StringUtils`
**Tipo:** Utility component (`@Component`).

**Metodi pubblici:**
- **`String normalizeString(String input)`**: `strip()` + lowercase con `Locale.ROOT`. Usato per normalizzare le email.
- **`String normalizeRole(String raw)`**: `trim()` + `toUpperCase()` (locale di default, non `Locale.ROOT` — incoerenza rispetto a `normalizeString`); antepone `"ROLE_"` se non già presente.

---

## Feature: Gestione Aziende (company)

### `com.pat.crewhive.company.AddressJSON`
**Tipo:** DTO (persistito come colonna `jsonb` tramite `@Type(JsonType.class)` dentro `Company`).

**Campi e validazioni:** `street` (`@NotBlank`, `@NoHtml`), `city` (`@NotBlank`, `@NoHtml`), `zipCode` (`@NotBlank`, `@Pattern("\\d{5}")`, `@NoHtml`), `province` (`@NotBlank`, `@Size(min=2,max=2)`, `@NoHtml`), `country` (`@NotBlank`, `@NoHtml`).

---

### `com.pat.crewhive.company.Company`
**Tipo:** Entity JPA, tabella `company`.

**Campi:**
- `companyId` (Long, PK, IDENTITY, setter disabilitato).
- `name` (String, `unique=true`) — sempre salvato normalizzato in minuscolo.
- `addressJSON` (`AddressJSON`) — colonna `jsonb`.
- `users` (`Set<User>`, `@OneToMany(mappedBy="company", LAZY)`).
- `companyType` (`CompanyType`, `@Enumerated(STRING)`).

**Costruttore:** `Company(CompanyRegistrationDTO registrationDTO)` — inizializza `name`, `addressJSON`, `companyType` dal DTO (senza normalizzare il nome, fatto poi dal service).

---

### `com.pat.crewhive.company.CompanyAccessService`
**Tipo:** Service (`@Service`).

**Scopo:** Bean separato per operazioni di `CompanyService` che devono passare per il proxy Spring (cache/transazioni), evitando problemi di self-invocation.

**Metodi pubblici:**
- `getCompanyById(Long companyId): Company` — `@Cacheable("companyById", key="#companyId")`. `ResourceAlreadyExistsException` se non trovata (nome eccezione fuorviante — di fatto "non trovata").
- `isNotPartOfCompany(Long userId, Long companyId): boolean` — `true` se l'utente non ha company o ha una company diversa. Controllo di autorizzazione centrale del package.
- `removeCompanyFromUsers(Long companyId): void` — `@Caching(evict=...)` su `usersInCompany`/`companyByUserId`. Imposta `company = null` per tutti gli utenti dell'azienda, passo preparatorio prima di eliminarla.

---

### `com.pat.crewhive.company.CompanyController`
**Tipo:** REST Controller, `/company`, implementa `CompanyControllerInterface`.

**Endpoint:**
1. `GET /company/{companyId}/users` — `ROLE_MANAGER`. Ritorna `List<UserIdAndNameAndHoursDTO>`.
2. `GET /company/{companyId}/user/{targetId}/info` — `ROLE_MANAGER`. Ritorna `UserWithTimeParamsDTO`.
3. `POST /company/register` — nessuna `@PreAuthorize` esplicita. Ritorna `AuthResponseDTO` (nuovo JWT, dato che il ruolo del richiedente diventa manager).
4. `PUT /company/set` — `ROLE_MANAGER`. Associa un utente alla company del manager.
5. `DELETE /company/{companyId}/remove/{userId}` — `ROLE_MANAGER`.
6. `DELETE /company/{companyId}/delete` — `ROLE_MANAGER`.

---

### `com.pat.crewhive.company.CompanyControllerInterface`
**Tipo:** Interfaccia di documentazione Swagger. Nessuna logica.

---

### `com.pat.crewhive.company.CompanyRegistrationDTO`
**Campi e validazioni:** `companyName` (`@NotBlank`, `@NoHtml`, `@Size(min=2,max=32)`), `companyType` (`@NotNull`), `address` (`AddressJSON`, `@Valid`).

---

### `com.pat.crewhive.company.CompanyRepository`
**Metodi:** `findByName(String)`, `findByUsers(Set<User>)` (non risulta usata), `existsByName(String)`.

---

### `com.pat.crewhive.company.CompanyService`
**Tipo:** Service — orchestratore principale della feature.

**Metodi pubblici:**

- `registerCompany(Long managerId, CompanyRegistrationDTO request): AuthResponseDTO` — `@Transactional`. Normalizza nome, `ResourceAlreadyExistsException` se duplicato, crea la company, assegna l'utente `managerId` come proprietario con ruolo globale `ROLE_MANAGER` (creato se assente — logica duplicata rispetto a `RoleService.getOrCreateGlobalRoleUser` per evitare dipendenza circolare, commentato esplicitamente nel codice), genera nuovo JWT+refresh token (necessario perché ruolo/company sono cambiati).
- `getCompanyById(Long companyId): Company` — delega a `companyAccessService`.
- `getCompanyByUserId(Long requestedUserId): Company` — `@Cacheable("companyByUserId")`.
- `getAllUsersInCompany(Long managerId, Long companyId): List<UserIdAndNameAndHoursDTO>` — `@Cacheable("usersInCompany")`. Verifica appartenenza manager (`AuthorizationDeniedException` altrimenti).
- `getCompanyUserWithInformation(Long managerId, Long companyId, Long targetId): UserWithTimeParamsDTO` — `@Cacheable("userInCompany")`. Stesso controllo di appartenenza.
- `setCompany(SetCompanyDTO request, Long companyId, Long managerId): void` — invalida cache multiple. Se l'utente target ha già una company, esce senza errore (idempotenza silenziosa).
- `deleteCompany(Long companyId, Long managerId): void` — scollega tutti gli utenti (`companyAccessService.removeCompanyFromUsers`), poi elimina l'azienda.
- `removeUserFromCompany(Long userId, Long managerId, Long companyId): void` — un manager non può auto-rimuoversi (`AuthorizationDeniedException`); delega a `userService.leaveCompany`.

---

### `com.pat.crewhive.company.CompanyType`
**Tipo:** Enum — `HOSPITAL`, `RESTAURANT`, `BAR`, `OTHER`, con `getLabel()`.

### `com.pat.crewhive.company.SetCompanyDTO`
**Campi:** `companyName` (`@NotBlank`, `@NoHtml`, `@Size(min=2,max=32)`), `userId` (`@NotNull`).

### `com.pat.crewhive.company.UserIdAndNameAndHoursDTO`
**Campi:** `userId`, `firstName`, `lastName`, `workableHoursPerWeek` (DTO di output, nessuna validazione).

---

## Feature: Gestione Ruoli e Manager (manager)

### `com.pat.crewhive.manager.ManagerController`
**Tipo:** REST Controller, `/manager`. Tutti gli endpoint richiedono `ROLE_MANAGER`.

**Endpoint:**
1. `POST /manager/create-role` — crea un ruolo per la company del manager autenticato.
2. `PATCH /manager/update-user-role` — riassegna un ruolo a un utente target.
3. `PATCH /manager/update-user-work-info` — aggiorna i parametri di lavoro di un utente (delega a `UserService`).
4. `DELETE /manager/delete-role/{roleName}` — elimina un ruolo.

Nessun endpoint verifica esplicitamente in questo controller che ruolo/utente target appartengano alla stessa azienda del manager oltre a passare il `companyId` del manager ai service — il controllo di coerenza è delegato interamente a `RoleService`/`UserService`.

---

### `com.pat.crewhive.manager.ManagerControllerInterface`
**Tipo:** Interfaccia Swagger (`@Tag("Manager Management")`). Nessuna logica.

---

### `com.pat.crewhive.manager.Role`
**Tipo:** Entity JPA, tabella `role`.

**Scopo:** Ruolo globale (`company == null`, es. `ROLE_USER`/`ROLE_MANAGER` di default) o specifico di un'azienda (ruoli custom).

**Campi:** `roleId` (PK, IDENTITY), `roleName`, `users` (`Set<UserRole>`, `@OneToMany(mappedBy="role", cascade=ALL, orphanRemoval=true)`), `company` (`@ManyToOne`, nullable).

**Vincoli:** unicità composita su (`role_name`, `company_id`) — stesso nome può esistere in aziende diverse, non duplicato nella stessa azienda.

---

### `com.pat.crewhive.manager.RoleRepository`
**Metodi:** `existsByRoleNameIgnoreCaseAndCompany`, `findByRoleNameIgnoreCaseAndCompany`, `findByRoleNameIgnoreCaseAndCompanyIsNull`, `existsByRoleNameIgnoreCaseAndCompanyIsNull` (non usato nei package esaminati), `findAllByCompany_CompanyId` (non usato nei package esaminati).

---

### `com.pat.crewhive.manager.RoleService`
**Tipo:** Service.

**Metodi pubblici:**

- `createRole(String roleName, Long companyId): void` — `@Transactional`. Normalizza il nome (`normalizeRole`), verifica azienda esistente, `ResourceAlreadyExistsException("Role already exists")` se duplicato per quella company, crea e salva.
- `updateUserRole(Long targetId, String newRole, Long companyId): void` — `@Transactional`. Cerca il ruolo nella company del manager (`ResourceNotFoundException` altrimenti — implica che non si può assegnare un ruolo esterno alla company del manager, né uno globale). Aggiorna l'associazione `UserRole` esistente via dirty-checking JPA (nessun `save` esplicito). **Nota**: non verifica esplicitamente che l'utente target appartenga alla `companyId` del manager chiamante — il controllo è implicito solo tramite l'appartenenza del ruolo alla company; punto potenzialmente delicato dell'autorizzazione, segnalato senza certezza che sia un difetto voluto.
- `deleteRole(String roleName, Long companyId): void` — `@Transactional`. `ResourceNotFoundException` se assente; `IllegalStateException("Cannot delete role because it is assigned to users")` se ha utenti assegnati.
- `getOrCreateGlobalRoleUser(): Role` — `@Transactional`. Cerca/crea il ruolo globale `ROLE_USER` (senza company). Usato in fase di registrazione utente.

---

### `com.pat.crewhive.manager.UpdateUserRoleDTO`
**Campi e validazioni:** `newRole` (`@NotBlank`, `@NoHtml`, e anomale `@Min(1)`/`@Max(15)` su un campo `String` — verosimile errore di copia/incolla, senza effetto pratico su Bean Validation), `userId` (annotato `@NotEmpty`, anch'essa pensata per collection/String, non per `Long` — probabile refuso, corretto sarebbe `@NotNull`).

### `com.pat.crewhive.manager.UpdateUserWorkInfoDTO`
**Campi e validazioni:** `targetUserId` (`@NotNull`, `@Positive`), `contractType` (`@NotNull`), `workableHoursPerWeek` (`@Min(0)`), `overtimeHours` (`@Min(0)`), `vacationDaysAccumulated`/`vacationDaysTaken`/`leaveDaysAccumulated`/`leaveDaysTaken` (`@Min(0)`, `@Digits(fraction=2, integer=3)`).

---

### `com.pat.crewhive.manager.UserRole`
**Tipo:** Entity JPA (tabella ponte), tabella `user_role`. Nonostante il nome, è una relazione **1:1** utente↔ruolo (non molti-a-molti), via `@MapsId`.

**Campi:** `userId` (PK, FK verso `User` via `@MapsId`), `user` (`@OneToOne(optional=false)`, `@JoinColumn(unique=true)`, `@MapsId`), `role` (`@ManyToOne(optional=false, LAZY)`).

**Metodi:**
- `equals(Object o)`: basato solo su `userId`.
- `hashCode()`: ritorna sempre `31` — scelta comune per entity JPA per evitare che l'hash cambi dopo la persistenza (problema noto con `HashSet`), al costo di performance ridotte con molte istanze.

### `com.pat.crewhive.manager.UserRoleId`
**Tipo:** `@Embeddable`, `Serializable` — chiave composita candidata (`userId`+`roleId`), pensata per un'eventuale futura evoluzione a ruoli multipli per utente (da un commento TODO nel codice).

**Stato attuale:** **non utilizzata da nessun'altra classe del codebase** — `UserRole` non la usa come `@EmbeddedId` (usa `@Id Long userId` + `@MapsId`, relazione 1:1 effettiva). Codice predisposto per estensione futura ma non collegato al resto del modello.

---

## Feature: Eventi (event)

### `com.pat.crewhive.event.Event`
**Tipo:** Entity JPA, tabella `event`, indici su `start_event`, `end_event`, `date`.

**Scopo:** Evento di calendario (pubblico o privato) con più partecipanti, tramite la entity di collegamento `EventUsers`.

**Campi:** `eventId` (PK, IDENTITY), `version` (`@Version`, optimistic locking), `eventName`, `description` (nullable), `start`/`end` (`OffsetDateTime`), `date` (`LocalDate`, derivata automaticamente da `start`), `color`, `eventType` (`EventType`, `@Enumerated(STRING)`), `users` (`Set<EventUsers>`, `@OneToMany(mappedBy="event", cascade=ALL, orphanRemoval=true)`).

**Annotazioni Jackson:** `@JsonIdentityInfo` su `eventId` (evita loop di serializzazione). Commento `//todo togli annotazioni json` segnala intenzione futura di rimuoverle (probabile passaggio a DTO di risposta).

**Metodi:**
- `addUser(User u): void` — aggiunge l'utente se non già presente (check via stream `anyMatch`), crea `EventUsers(u, this)`, aggiorna entrambi i lati della relazione.
- `removeUser(User u): void` — rimuove il link bidirezionale, azzera i riferimenti incrociati (attiva `orphanRemoval`).
- `Event(Set<User> user, String name, String description, OffsetDateTime startEvent, OffsetDateTime endEvent, String color, EventType eventType)` — costruttore di comodo che chiama `addUser` per ogni utente, poi `syncDate()`. Non risulta usato da `EventService`, che costruisce con `new Event()` + setter.
- `private syncDate(): void` (`@PrePersist @PreUpdate`) — imposta `date = start.toLocalDate()`.

---

### `com.pat.crewhive.event.EventUsers`
**Tipo:** Entity JPA (join), tabella `event_users`, vincolo unicità su `(event_id, user_id)`.

**Campi:** `id` (`EventUsersId`, `@EmbeddedId`), `user` (`@ManyToOne(optional=false, LAZY)`, `@MapsId("userId")`, `@JsonBackReference`), `event` (`@ManyToOne(optional=false, LAZY)`, `@MapsId("eventId")`, `@JsonBackReference`).

**Costruttore:** `EventUsers(User user, Event personalEvent)` — la chiave composita è derivata automaticamente via `@MapsId`.

### `com.pat.crewhive.event.EventUsersId`
**Tipo:** `@Embeddable`, `Serializable` — chiave composita `(userId, eventId)`.

### `com.pat.crewhive.event.EventType`
**Tipo:** Enum — `PUBLIC("Public")`, `PRIVATE("Private")`, con `getLabel()`.

---

### `com.pat.crewhive.event.CreateEventDTO`
**Campi e validazioni:** `name` (`@NotBlank`, `@NoHtml`, `@Size(min=3,max=32)`), `description` (`@NoHtml`, `@Size(max=256)`, opzionale), `start`/`end` (`@NotNull`), `color` (`@NotBlank`, `@NoHtml`, `@Size(min=6,max=6)`), `eventType` (`@NotNull`), `userId` (`Set<Long>`, `@NotNull`).

### `com.pat.crewhive.event.PatchEventDTO`
**Campi e validazioni:** `eventId` (`@NotNull`), `name` (`@NotBlank`, `@NoHtml`, nessun `@Size` — a differenza di `CreateEventDTO`), `description` (`@NoHtml`, `@Size(max=100)` — limite diverso da creazione, che è 256), `start`/`end` (`@NotNull`), `color` (`@NotBlank`, `@NoHtml`, `@Size(min=6,max=6)`), `eventType` (`@NotNull`), `userId` (`Set<Long>`, nessuna validazione — se `null` i partecipanti non vengono toccati).

---

### `com.pat.crewhive.event.EventRepository`
**Metodi:**
- `findWithParticipantsByUserAndDateBetween(Long userId, LocalDate from, LocalDate to)`: `@EntityGraph({"users","users.user"})`, eventi distinti dell'utente nel range, ordinati per `start`.
- `findPublicWithParticipantsByCompanyAndDateBetween(EventType, Long companyId, LocalDate from, LocalDate to)`: analoga, filtrata per tipo evento e company dei partecipanti.
- `findByIdWithParticipants(Long id)`: singolo evento con partecipanti precaricati.

### `com.pat.crewhive.event.EventUsersRepository`
**Metodi:**
- `findEventsByUserId(Long userId)`: eventi collegati a un utente, ordinati per `start` (nessun `@EntityGraph` qui, a differenza di `EventRepository`).
- `deleteByEventId(Long eventId)`: `@Modifying(clearAutomatically=true, flushAutomatically=true)`, cancella tutti i link `EventUsers` di un evento.

---

### `com.pat.crewhive.event.EventService`
**Tipo:** Service.

**Metodi pubblici:**

- `createEvent(CreateEventDTO dto, String role): Long` — `@Transactional`. Normalizza nome; `IllegalArgumentException` se `start > end`; se `role == "ROLE_USER"` e `eventType == PUBLIC`, `AuthorizationDeniedException("Non sei autorizzato a creare eventi pubblici")` (solo ruoli diversi da `ROLE_USER` possono creare eventi pubblici); risolve gli utenti, crea l'evento, salva, ritorna l'id.
- `getEventsByPeriodAndUser(Period period, Long userId): List<Event>` — readOnly. Calcola `from`/`to` con `DateUtils`. Commento `//todo ritorna un DTO`.
- `getUserEvents(Long userId): List<Event>` — readOnly. Tutti gli eventi (senza filtro temporale).
- `getPublicEventsByCompanyAndPeriod(Long companyId, Period period): List<Event>` — readOnly. Solo eventi `PUBLIC`.
- `patchEvent(PatchEventDTO dto): Long` — `@Transactional`. Valida `start`/`end`; carica l'evento (`ResourceNotFoundException` altrimenti); aggiorna i campi; se `userId` non è `null`, calcola diff (`toRemove`/`toAdd`) tra partecipanti attuali e richiesti e sincronizza; se `userId` è `null`, i partecipanti non vengono toccati.
- `deleteEvent(Long eventId): void` — `@Transactional`. Verifica esistenza (`ResourceNotFoundException` altrimenti), cancella esplicitamente i link `EventUsers`, poi l'evento.

---

### `com.pat.crewhive.event.EventControllerInterface`
**Tipo:** Interfaccia Swagger. A differenza di `ShiftTemplateControllerInterface`, non documenta esplicitamente 404 sugli endpoint eventi.

### `com.pat.crewhive.event.EventController`
**Tipo:** Controller REST, `/event`.

**Endpoint:**
- `POST /event/create` — `createEvent(cud, @RequestBody @Valid CreateEventDTO)`: usa `cud.getRole()` per il controllo su eventi pubblici (fatto nel service, non `@PreAuthorize`).
- `GET /event/{temp}/user/{userId}` — nessun controllo che `userId` corrisponda all'utente autenticato.
- `GET /event/user/{userId}` — stessa osservazione.
- `GET /event/public/{temp}` — usa `cud.getCompanyId()`, quindi vincolato alla company dell'utente autenticato.
- `PATCH /event/patch` — nessun controllo di autorizzazione esplicito nel controller.
- `DELETE /event/delete/{eventId}`.

**Nota generale:** a differenza di `ShiftTemplateController`, `EventController` non usa `@PreAuthorize`; eventuali restrizioni sono affidate alla logica applicativa nel service o a configurazioni globali non presenti in questo package.

---

## Feature: Template Turni (shifttemplate)

### `com.pat.crewhive.shifttemplate.ShiftTemplate`
**Tipo:** Entity JPA, tabella `shift_template`, vincolo unicità su `(shift_name, company_id)`.

**Campi:** `shiftId` (PK, IDENTITY), `shiftName`, `startShift`/`endShift` (`OffsetTime`), `description`, `color`, `company` (`@ManyToOne(LAZY, optional=false)`).

---

### `com.pat.crewhive.shifttemplate.CreateShiftTemplateDTO`
**Campi e validazioni:** `shiftName` (`@NotBlank`, `@NoHtml`, `@Size(1,32)`), `description` (`@NoHtml`, `@Size(1,255)`, non `@NotBlank`), `color` (`@NotBlank`, `@NoHtml`, `@Size(6,6)`), `start`/`end` (`@NotNull`), `companyId` (`@NotNull`).

### `com.pat.crewhive.shifttemplate.PatchShiftTemplateDTO`
**Tipo:** `extends CreateShiftTemplateDTO`. Campo aggiuntivo: `oldShiftName` (`@NotBlank`, `@NoHtml`, `@Size(3,32)`) — identifica il template esistente da patchare (anche l'autore originale segnala incertezza sull'intento esatto in un commento TODO nel codice).

---

### `com.pat.crewhive.shifttemplate.ShiftTemplateRepository`
**Metodi:** `findByShiftNameAndCompanyCompanyId`, `existsByShiftNameAndCompanyCompanyId`.

---

### `com.pat.crewhive.shifttemplate.ShiftTemplateService`
**Tipo:** Service.

**Metodi pubblici:**

- `getShiftTemplate(String shiftName, Long companyId): ShiftTemplate` — readOnly. Normalizza il nome; `ResourceNotFoundException` se assente. Commento `//todo ritorna un dto`.
- `createShiftTemplate(CreateShiftTemplateDTO dto): ShiftTemplate` — `@Transactional`. Controllo duplicati eseguito su `dto.getShiftName()` **non ancora normalizzato** mentre l'entity viene poi salvata con il nome normalizzato — potenziale incoerenza se la normalizzazione cambia la stringa. `ResourceAlreadyExistsException` se duplicato.
- `patchShiftTemplate(PatchShiftTemplateDTO dto): ShiftTemplate` — `@Transactional`. Normalizza entrambi i nomi; se il nuovo nome collide con un template esistente diverso da quello in modifica (`oldShiftName != shiftName`), `ResourceAlreadyExistsException`; carica il template esistente (`ResourceNotFoundException` altrimenti); aggiorna i campi.
- `deleteShiftTemplate(String shiftName, Long companyId): void` — `@Transactional`. `ResourceNotFoundException` se assente (messaggio con nome non normalizzato — incoerenza minore).

---

### `com.pat.crewhive.shifttemplate.ShiftTemplateControllerInterface`
**Tipo:** Interfaccia Swagger — documenta esplicitamente 404 e 409, coerentemente con le eccezioni del service.

### `com.pat.crewhive.shifttemplate.ShiftTemplateController`
**Tipo:** Controller REST, `/shift-template`. **Tutti gli endpoint protetti da `@PreAuthorize("hasRole('ROLE_MANAGER')")`**.

**Endpoint:**
- `GET /shift-template/get/{shiftName}/company/{companyId}`.
- `POST /shift-template/create`.
- `PATCH /shift-template/update`.
- `DELETE /shift-template/delete/{shiftName}/company/{companyId}`.

**Nota:** a differenza di `EventController`, qui l'autorizzazione basata sul ruolo è dichiarativa a livello di controller (`@PreAuthorize`), non dentro il service.

---

## Feature: Turni Programmati (shiftprogrammed)

Gestisce i turni pianificati (calendario futuro), con assegnazione multi-utente tramite tabella di join, ricerca per periodo/utente/azienda con **caching Redis**, creazione, modifica e cancellazione.

### `com.pat.crewhive.shiftprogrammed.CacheKeys`
**Tipo:** Classe di utilità (final, costruttore privato) — generatore di chiavi cache.

**Metodi:**
- `static String shiftsByUser(Long userId, Period period)`: ritorna `userId + ":" + period`.
- `static String shiftsByCompany(Long userId, Period period)`: identica implementazione (nota: il parametro si chiama `userId` ma rappresenta il `requesterUserId`).

**Nota di coerenza:** invocata solo nelle espressioni SpEL di `@CacheEvict` in `ShiftProgrammedService`, mentre `@Cacheable` scrive la chiave manualmente come stringa SpEL invece di richiamare `CacheKeys` — duplicazione logica, formato comunque identico, ma potenziale fonte di disallineamento futuro.

### `com.pat.crewhive.shiftprogrammed.CreateShiftProgrammedDTO`
**Campi e validazioni:** `name` (`@NotBlank`, `@NoHtml`, `@Size(3,32)`), `description` (`@NoHtml`, `@Size(max=256)`, opzionale), `start`/`end` (`@NotNull`), `color` (`@NotBlank`, `@NoHtml`, `@Size(6,6)`), `userId` (`Set<Long>`, nessuna validazione esplicita).

### `com.pat.crewhive.shiftprogrammed.NameAndUserIdForShiftProgrammedDTO`
**Scopo:** liste parallele (`firstName`, `lastName`, `userId`, stesso indice = stesso utente) + `shiftProgrammedId`. Design segnalato dal codice stesso come da rifattorizzare (vedi TODO in `ShiftProgrammedOutputDTO`).

### `com.pat.crewhive.shiftprogrammed.PatchShiftProgrammedDTO`
**Campi e validazioni:** `shiftProgrammedId` (`@NotNull`), `name` (`@NotBlank`, `@NoHtml`, nessun `@Size` — incoerenza rispetto a `CreateShiftProgrammedDTO`), `description` (`@NoHtml`, `@Size(max=100)` — diverso dal max=256 di creazione), `start`/`end` (`@NotNull`), `color` (`@NotBlank`, `@NoHtml`, `@Size(6,6)`), `userId` (`Set<Long>`, se valorizzato ridefinisce i partecipanti).

---

### `com.pat.crewhive.shiftprogrammed.ShiftProgrammed`
**Tipo:** Entity JPA, tabella `shift_programmed`, indici su `shift_date`, `start_shift`.

**Campi:** `shiftProgrammedId` (PK, IDENTITY), `version` (`@Version`), `shiftName`, `start`/`end`, `date` (derivata da `start`), `description`, `color`, `users` (`Set<ShiftUser>`, `@OneToMany(mappedBy="shift", cascade=ALL, orphanRemoval=true)`, `@JsonManagedReference`).

**Metodi:**
- `Costruttore ShiftProgrammed(Set<User> user, ...)` — non usato da `ShiftProgrammedService.createShift` (che usa costruttore vuoto + setter, probabile codice residuo/alternativo).
- `addUser(User u): void` — aggiunge se non già presente, mantiene coerenti entrambi i lati.
- `removeUser(User u): void` — rimuove il link e annulla riferimenti incrociati.
- `private syncDate()` (`@PrePersist @PreUpdate`) — sincronizza `date` con `start`.

---

### `com.pat.crewhive.shiftprogrammed.ShiftProgrammedController`
**Tipo:** Controller REST, `/shift-programmed`.

**Endpoint:**
- `POST /shift-programmed/create` — usa l'utente autenticato come `creatorUserId` (solo per invalidazione cache, nessun controllo di autorizzazione esplicito).
- `GET /shift-programmed/period/{period}/user/{userId}` — nessun controllo che l'utente autenticato coincida con `userId`.
- `GET /shift-programmed/period/{period}/company}` — **nota**: il path contiene un refuso, la graffa `}` finale è parte letterale del path.
- `GET /shift-programmed/users/{shiftId}`.
- `PATCH /shift-programmed/patch`.
- `DELETE /shift-programmed/delete/{shiftId}`.

**Autorizzazione:** nessun `@PreAuthorize` esplicito a livello di controller; tutti gli endpoint richiedono comunque un Bearer token valido (autenticazione, non autorizzazione per ruolo).

### `com.pat.crewhive.shiftprogrammed.ShiftProgrammedControllerInterface`
**Tipo:** Interfaccia Swagger. Nessuna logica.

### `com.pat.crewhive.shiftprogrammed.ShiftProgrammedOutputDTO`
**Campi:** `shifts` (`List<ShiftProgrammed>` — espone direttamente l'entity JPA, non un DTO puro; TODO nel codice per un DTO dedicato), `users` (`List<NameAndUserIdForShiftProgrammedDTO>`, stesso ordine di `shifts`).

---

### `com.pat.crewhive.shiftprogrammed.ShiftProgrammedRepository`
**Metodi:**
- `findByIdWithWorkers(Long id)`: `@EntityGraph({"users.user"})`.
- `findByUserAndDateBetween(Long userId, LocalDate from, LocalDate to)`: JPQL `distinct`, ordinata per `start`.
- `findByCompanyAndDateBetween(Long companyId, LocalDate from, LocalDate to)`: analoga, filtrata per company.

---

### `com.pat.crewhive.shiftprogrammed.ShiftProgrammedService`
**Tipo:** Service — logica con **caching Redis**.

**Metodi pubblici:**

- `createShift(Long creatorUserId, CreateShiftProgrammedDTO dto): Long` — `@Transactional`, `@Caching(evict=...)` su `shiftsByUser`/`shiftsByCompany` per `creatorUserId`, solo periodi **DAY/WEEK/MONTH** (non TRIMESTER/SEMESTER/YEAR — possibile incompletezza: viste in cache per quei periodi potrebbero restare stale). Valida `start ≤ end`, risolve utenti, salva, ritorna l'id.
- `getShiftsByPeriodAndUser(Period period, Long userId): ShiftProgrammedOutputDTO` — `@Cacheable("shiftsByUser", key="userId:period")`. Costruisce liste parallele nome/cognome/id per turno.
- `getShiftsByPeriodAndCompany(Period period, Long requesterUserId): ShiftProgrammedOutputDTO` — `@Cacheable("shiftsByCompany", key="requesterUserId:period")`. La chiave si basa sull'utente richiedente, non sull'azienda: utenti diversi della stessa company generano voci di cache distinte anche per lo stesso risultato (meno efficiente ma corretto).
- `getUsersInShift(Long shiftId): List<User>` — `@Cacheable("usersInShift", key="shiftId")`. `ResourceNotFoundException` se il turno non esiste.
- `patchShift(Long requesterUserId, PatchShiftProgrammedDTO dto): Long` — `@Transactional`. Se `userId` è un Set vuoto (non null), rimuove **tutti** gli utenti assegnati; se `null`, non tocca la relazione — comportamento distinto tra "non specificato" e "svuota". Evict di `usersInShift` + `shiftsByUser`/`shiftsByCompany` (DAY/WEEK/MONTH).
- `deleteShift(Long requesterUserId, Long shiftId): void` — `@Transactional`. `ResourceNotFoundException` se assente. Cancellazione esplicita di `ShiftUser` (ridondante rispetto a `orphanRemoval`, ma eseguita comunque, probabilmente per bulk delete via query diretta). Stessa strategia di evict di `patchShift`.

---

### `com.pat.crewhive.shiftprogrammed.ShiftUser`
**Tipo:** Entity JPA (join), tabella `shift_user`, vincolo unicità su `(shift_programmed_id, user_id)`.

**Campi:** `id` (`ShiftUserId`, `@EmbeddedId`), `shift` (`@ManyToOne(optional=false, LAZY)`, `@MapsId`, `@JsonIgnore`+`@JsonBackReference`), `user` (idem).

### `com.pat.crewhive.shiftprogrammed.ShiftUserId`
**Tipo:** `@Embeddable`, `Serializable` — `(shiftProgrammedId, userId)`.

### `com.pat.crewhive.shiftprogrammed.ShiftUserRepository`
**Metodi:**
- `findUsersByShiftId(Long shiftId)`: `distinct`, ordinata per `username`.
- `deleteByShiftId(Long shiftId)`: `@Modifying`.
- `deleteByUserId(Long userId)`: `@Modifying` (non usata nei package shiftprogrammed/shiftworked esaminati — probabilmente usata da `UserService.leaveCompany`/`deleteAccount`).

---

## Feature: Turni Lavorati (shiftworked)

Gestisce la registrazione consuntiva delle ore effettivamente lavorate, con calcolo automatico delle ore nette e accumulo dello straordinario sul profilo utente. Nessuna cache Redis in questo package.

### `com.pat.crewhive.shiftworked.CreateShiftWorkedDTO`
**Campi e validazioni:** `shiftName` (`@NotBlank`, `@NoHtml`, `@Size(3,32)`), `start`/`end` (`@NotNull`), `breakTime` (`int`, annotato `@NotNull` — **inefficace su tipo primitivo**, probabile refuso: andrebbe `Integer` o rimossa l'annotazione), `extraHours` (`BigDecimal`, `@NotNull`), `userId` (`@NotNull`).

---

### `com.pat.crewhive.shiftworked.ShiftWorked`
**Tipo:** Entity JPA, tabella `shift_worked`, indici su `user_id`, `start_shift`, `end_shift`, `shift_date`.

**Costanti:** `HOURS_SCALE = 2`, `HOURS_ROUNDING = HALF_UP`.

**Campi:** `shiftWorkedId` (PK, IDENTITY), `shiftName`, `start`/`end`, `date` (derivata), `breakTime` (int, minuti), `workedHours` (BigDecimal, calcolato automaticamente — mai impostato manualmente dall'esterno), `extraHours` (BigDecimal), `user` (`@ManyToOne(optional=false, LAZY)`).

**Metodi:**
- Costruttore `ShiftWorked(shiftName, start, end, breakTimeMinutes, extraHours, user)` — imposta `extraHours = ZERO` se `null`, calcola `workedHours` con `computeWorkedHours`.
- `private static BigDecimal minutesToHours(long minutes)` — conversione con scala 2, `HALF_UP`.
- `private static BigDecimal computeWorkedHours(start, end, breakMinutes)`: ritorna `ZERO` se `start`/`end` nulli; `IllegalArgumentException` se `end ≤ start`; calcola `net = hoursTotal - breakHours`; ritorna `net` se positivo, altrimenti `ZERO` (evita ore negative).
- `private void syncAndRecompute()` (`@PrePersist @PreUpdate`) — risincronizza `date` e **ricalcola sempre** `workedHours`, garantendo coerenza anche se i campi sono stati modificati dopo la costruzione; forza `extraHours = ZERO` se `null`.
- `private boolean isChronologicallyValid()` (`@AssertTrue`) — vincolo Bean Validation, verifica `start`/`end` non nulli e `end > start` (rete di sicurezza aggiuntiva rispetto al controllo in `computeWorkedHours`).

---

### `com.pat.crewhive.shiftworked.ShiftWorkedController`
**Tipo:** Controller REST, `/shift-worked`.

**Endpoint:**
- `POST /shift-worked/create` — `createShiftWorked(@RequestBody @Valid CreateShiftWorkedDTO)`: `200 OK` con testo "ShiftWorked created successfully". **Nota**: a differenza del controller `shiftprogrammed`, non è presente `@AuthenticationPrincipal` — si basa unicamente sull'`userId` nel body, senza verificare che l'utente autenticato coincida o abbia il permesso di registrare turni per conto di un altro utente.

### `com.pat.crewhive.shiftworked.ShiftWorkedControllerInterface`
**Tipo:** Interfaccia Swagger. Nessuna logica.

### `com.pat.crewhive.shiftworked.ShiftWorkedRepository`
**Tipo:** Repository — nessun metodo custom (solo CRUD standard di `JpaRepository`).

### `com.pat.crewhive.shiftworked.ShiftWorkedService`
**Tipo:** Service.

**Metodo pubblico:**
- `createShiftWorked(CreateShiftWorkedDTO dto): void` — `@Transactional`. Recupera l'utente, normalizza il nome turno, costruisce l'entity (calcola `workedHours` automaticamente), somma `dto.getExtraHours()` a `user.getOvertimeHours()` e salva l'utente, poi salva il turno.
  - **Effetto collaterale rilevante:** ogni turno lavorato incrementa cumulativamente `overtimeHours` dell'utente con l'importo `extraHours` dichiarato nel DTO, **indipendentemente** dalle `workedHours` effettivamente calcolate — nessuna validazione che le ore di straordinario dichiarate siano plausibili rispetto alla durata del turno.
  - **Nessun controllo di concorrenza esplicito** sul read-modify-write di `overtimeHours`: due richieste concorrenti per lo stesso utente potrebbero causare una race condition con perdita di aggiornamenti, salvo meccanismi di versioning non visibili in questo service.

---

## Feature: Configurazione Applicativa (config, swagger, bootstrap)

### `com.pat.crewhive.HoursCalculatorApplication`
**Tipo:** Entry point Spring Boot.

**Annotazioni:** `@SpringBootApplication`, `@EnableScheduling` (necessaria per `MonthlyVacationCron`/`MonthlyLeaveDaysCron`).

**Metodo:** `static void main(String[] args)` — `SpringApplication.run(HoursCalculatorApplication.class, args)`.

---

### `com.pat.crewhive.api.swagger.config.OpenApiConfig`
**Tipo:** `@Configuration`.

**Scopo:** Configura Swagger/OpenAPI: schema di sicurezza `@SecurityScheme(name="bearerAuth", type=HTTP, scheme="bearer", bearerFormat="JWT")`, richiamato da `@SecurityRequirement(name="bearerAuth")` nelle interfacce controller.

**Bean:** `OpenAPI openAPI()` — `Info`: titolo "CrewHive API", descrizione "Documentazione API con JWT Bearer", versione "v1".

---

### `com.pat.crewhive.api.swagger.schema.ApiError`
**Tipo:** DTO (record), schema di documentazione per le risposte di errore (formato RFC 7807 + campi custom). Usato solo a scopo di documentazione OpenAPI (referenziato nei `@Content(schema=...)` delle `@ApiResponse`), non istanziato direttamente nei file letti (la generazione reale è nel `GlobalExceptionHandler`).

**Campi:** `type`, `title`, `status` (Integer), `detail`, `instance`, `timestamp`, `errorCode` (custom), `errors` (`Map<String,String>`, custom).

---

### `com.pat.crewhive.config.cache.RedisCacheConfig`
**Tipo:** `@Configuration`, `@EnableCaching`.

**Scopo:** Configura il comportamento della cache Redis (usata da `CompanyService`, `ShiftProgrammedService`, ecc.).

**Bean:** `RedisCacheConfiguration cacheConfiguration()`:
1. Crea un `BasicPolymorphicTypeValidator` che consente la deserializzazione polimorfica solo per i prefissi `com.pat.crewhive.`, `java.util.`, `java.lang.`, `java.math.`, `java.time.` — mitigazione contro attacchi di tipo "deserialization gadget".
2. `entryTtl(Duration.ofMinutes(30))`, `disableCachingNullValues()`, chiavi serializzate come stringhe semplici, valori serializzati in JSON con `enableDefaultTyping(typeValidator)`.
3. Ritorna la configurazione di default per tutte le cache Redis dichiarate nell'applicazione.

**Nota:** gli import Jackson qui provengono dal package `tools.jackson` (Jackson 3.x), mentre l'entity `User` usa `com.fasterxml.jackson.annotation.*` (Jackson classico) — coesistenza di due famiglie di API Jackson nel progetto, probabile fase di migrazione o dipendenza transitiva; non determinabile con certezza se intenzionale.
