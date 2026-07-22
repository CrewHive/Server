# 🚀 CrewHive - Server

### 🎯 Obiettivo del progetto
Backend **RESTful** della piattaforma **CrewHive**, dedicato alla **gestione di turni, eventi e presenze** in contesti aziendali.  
Fornisce servizi a client **mobile** e **web** per manager e dipendenti, centralizzando **autenticazione**, **autorizzazione** e orchestrazione del dominio.

---

### 🛠️ Stack Tecnologico
- **Java + Spring Boot** → architettura a livelli *(Controller → Service → Repository)*
- **PostgreSQL** con **JPA/Hibernate** per la persistenza
- **Jakarta Validation** per validazione DTO
- **Maven** (`pom.xml` + wrapper) per build e gestione dipendenze

---

### 🔐 Sicurezza e Accesso
- **Spring Security + JWT** (access & refresh token) per autenticazione **stateless**
- Gestione di **ruoli** e **claim personalizzati** per autorizzazioni lato API
- **Global Exception Handling** e logging strutturato con tracciabilità delle richieste
- Documentazione API tramite **OpenAPI / Swagger**

---

### 📏 Stile e Qualità del Progetto
- Convenzioni **REST** chiare e consistenti
- DTO con **validazione** e sanificazione input dove necessario
- Ottimizzazione delle query (evitare **N+1 problem** con fetch mirati)
- **Indici** a livello DB per performance scalabili
- Logging strutturato per debugging e diagnosi

---
