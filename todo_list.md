
## TODO LIST

---

* Crea i test per TUTTE le funzioni dei service;
* Crea i DTO per le funzioni segnate con //todo;
  * ✅ Event (`EventOutputDTO`/`EventParticipantDTO`) e ShiftProgrammed (`ShiftProgrammedItemDTO`/`ShiftParticipantDTO`), con rimozione di `@JsonIgnore`/`@JsonManagedReference`/`@JsonBackReference`/`@JsonIdentityInfo` da `Event`, `EventUsers`, `ShiftProgrammed`, `ShiftUser`, `User`;
  * ⬜ `RoleService` (riga con `//todo Ritorna un DTO`);
  * ⬜ `ShiftTemplateService` (3 righe con `//todo ritorna un dto`) — `ShiftTemplateController` ritorna ancora l'entity `ShiftTemplate`;
* Integra la cache di Redis nelle varie funzioni dei service;

---
