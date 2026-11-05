# Work Log (WORKLOG.md)

This document keeps a chronological record of work sessions, in-progress tasks, important
decisions, and pending items. This ensures context is never lost between sessions.

## Session: 2026-11-04 (Role-Based Access Control, Supabase OAuth Sync & Database Hardening)

**What we implemented this session:**
- **Database & Supabase OAuth Sync Trigger**:
  - Restored and updated `database/schema_backup.sql` with the `rol VARCHAR(20) DEFAULT 'USER' NOT NULL` column and `usuario_email_key` unique constraint.
  - Authored `database/migrations/V1__add_rol_and_supabase_oauth_sync.sql` providing an idempotent migration script and the `public.handle_new_user()` trigger for automated Google OAuth sync from `auth.users` to `public.usuario`.
- **Spring Boot RBAC & Authorization Layer**:
  - Created `buss.smartbussingapi.Usuario.Rol` enum (`USER`, `ADMIN`).
  - Added `rol` attribute in `Usuario.java` and `UsuarioDTO.java`, updating `UsuarioService` and `GlobalControllerAdvice`.
  - Created `buss.smartbussingapi.Usuario.RoleAuthorizationService` with real-time MDC security auditing (`unauthorized_admin_access`, HTTP 403) for Wazuh SIEM detection.
  - Protected administrative endpoints (`POST /api/v1/ruta`, `POST /api/v1/parada`, `POST /api/v1/registrarLugar`, and `GET /api/v1/user`).
- **Web Dashboard Authorization & Supervisor Guard**:
  - Updated `AuthContext.jsx` in `smart-bussing` web to query backend role for the authenticated Google account.
  - Enhanced `ProtectedRoute.jsx` with an explicit "Acceso Denegado" view for non-admin accounts.
  - Injected `X-User-Email` header in `NewRoutePage.jsx` when submitting new GeoJSON route payloads.
- **Test Suite Verification**:
  - Created `RoleAuthorizationServiceTest.java` (5 unit tests).
  - Updated `UsuarioControllerTest.java` and `UsuarioServiceTest.java` (134 total unit tests passing with 0 failures).

**What is in progress:**
- Executing the SQL migration in Supabase and assigning the initial admin email.

**Blockers:**
- None.

**Next steps in priority order:**
1. Paste and run `database/migrations/V1__add_rol_and_supabase_oauth_sync.sql` in the Supabase SQL Editor.
2. Promote the primary supervisor to administrator via `UPDATE public.usuario SET rol = 'ADMIN' WHERE email = 'tu_correo@cetys.mx';`.
3. Log in via Google OAuth on the Web Dashboard and verify route creation.

## Session: 2026-11-04 (DataFog Core DLP, Content Moderation & Wazuh SIEM Integration)

**What we implemented this session:**
- **Gitleaks Secret Scanning**: Configured `.gitleaks.toml` to detect Mapbox secret tokens (`sk.*`) and PostgreSQL credentials, while allowlisting `.dummy` mock tokens in test resources.
- **DataFog Core Sidecar Microservice**: Created `dlp-service/` (FastAPI, Dockerfile, `banned_words.txt`, `export_anonymizer.py`, and `docker-compose-dlp.yml`) for offline PII detection and profanity filtering.
- **Spring Boot DLP Validation Service**: Implemented `DlpValidationService.java` with Spring `@Autowired` injection, timeouts via `SimpleClientHttpRequestFactory`, and local fallback check if the sidecar service is offline.
- **Route Report Interception**: Integrated DLP validation inside `ReporteRutaService.java` when creating route reports (`POST /api/v1/reporteRuta/{id_ruta}`), throwing `DlpViolationException` before database persistence.
- **Information Leakage & Error Sanitization (CWE-209)**: Hardened `application.properties` with `server.error.include-message=never` and updated `GlobalControllerAdvice.java` to sanitize HTTP 500 error messages and cleanly handle malformed JSON.
- **Security Audit & Wazuh Logging**: Refined `SecurityAuditFilter.java` to prevent overwriting `security_alert` on HTTP 400 responses, ensuring structured JSON logs (`event_type: "security_alert"`, `threat_type: "OBSCENE_CONTENT"`) flow into `logs/smart-bussing.json`.
- **Wazuh SIEM Rules**: Documented Policy 4, rules `100110` (DLP Alert Level 10) and `100111` (Flood/Abuse Level 12), and `wazuh-logtest` test cases in `README.md`.
- **Architecture Flow Diagrams Skill**: Created `AGENT_CONTEXT/skill-flow-diagrams/SKILL.md` standardizing Mermaid sequence diagrams for system lifecycle and request tracing.
- **Comprehensive Test Suite**: Added `DlpValidationServiceTest.java` and updated `ReporteRutaServiceTest.java` (129 unit tests passing with 0 failures).

**What is in progress:**
- Ready for deployment and verification on staging/production environments.

**Blockers:**
- None.

**Next steps in priority order:**
1. Start the DataFog sidecar service (`docker compose -f docker-compose-dlp.yml up -d` or `uvicorn main:app --port 8000`).
2. Add custom regional slang / banned words to `dlp-service/banned_words.txt` if needed.
3. Verify rule `100110` firing in Wazuh Dashboard with the `wazuh-logtest` payload from `README.md`.

## Session: 2026-05-04 (Integration Testing & Agent Setup)

**What we implemented this session:**
- Generated `AGENT_CONTEXT` core files (`CLAUDE.md`, `SPEC.md`, `WORKLOG.md`, `REFERENCES.md`).
- Established the `generate-integration-tests` skill to standardize testing.
- Created comprehensive integration tests for all controllers (`Usuario`, `Viaje`, `Parada`, `Lugar`, `Interesado`, `RegistroLugar`, `ReporteRuta`, `Ruta`).
- Refined the test suite to execute against a containerized PostgreSQL instance (`docker-compose-test.yml`).
- Fixed a major JSON infinite recursion bug in `Coordenadas` that prevented proper endpoint serialization.

**What is in progress:**
- Transitioning to feature development now that test harnesses are in place.

**Blockers:**
- None.

**Next steps in priority order:**
1. Start implementing the first planned `/new-feature` with the testing workflow verified.
2. Consider adding CI/CD pipelines to run the Docker tests automatically.

## Session: 2026-05-04 (Initial Setup)

**What we implemented this session:**
- Created the four agent context files: `CLAUDE.md`, `SPEC.md`, `WORKLOG.md`, `REFERENCES.md`.
- Defined the project architecture and initial tech stack.
- Established custom agent commands.

**What is in progress:**
- Initializing the core database schema and JPA entities.

**Blockers:**
- None.

**Next steps in priority order:**
1. Verify local dev servers start without errors (frontend + backend).
2. Set up the database schema with initial models.
3. Implement the first feature with `/new-feature`.

---
*(Add new sessions above this line to maintain reverse-chronological order.)*
