# SKILL: Architecture & Request Flow Diagrams

---
name: architecture-flow-diagrams
description: "Guidelines and standards for generating clear, comprehensive Mermaid sequence and flow diagrams documenting backend request lifecycles, security filters, microservices, and database persistence for Smart Bussing."
---

## Purpose
Document end-to-end request lifecycles across architectural layers in Smart Bussing. Whenever a new feature, security filter, or endpoint workflow is built or refactored, use this skill to generate a structured, visually clear Mermaid diagram showing both the happy path and rejection/failure branches.

## Architectural Layers to Represent
When charting any endpoint in Smart Bussing, always represent the participants in logical sequence:

1. **Cliente / Actor**: App Móvil (React Native), Web o Cliente API (Postman/Curl).
2. **Filtros HTTP / Middlewares (`SecurityAuditFilter`)**: Inspección de URI, headers de seguridad, Rate Limiting y contexto inicial MDC.
3. **Controlador (`*Controller`)**: Deserialización del payload JSON a DTO y validación de entrada.
4. **Servicio de Negocio (`*Service`)**: Orquestación de reglas de negocio, autorizaciones y bloqueos de recursos.
5. **Interceptores / Validadores / Microservicios (Sidecar / DLP / Mapbox)**: Motores especializados de validación o enriquecimiento de datos.
6. **Capa de Logging & SIEM (`logs/smart-bussing.json` -> Wazuh)**: Trazabilidad estructurada de eventos y alertas de seguridad.
7. **Capa de Persistencia (PostgreSQL / JGraphT)**: Base de datos transaccional o grafos en memoria.

## Diagram Rules & Standards
1. **Diagram Type**: Always prefer `sequenceDiagram` for request/response lifecycles, and `flowchart LR` / `flowchart TD` for module architecture.
2. **Numbering**: Always include `autonumber` in sequence diagrams to ease step-by-step review.
3. **Visual Highlighting**: Use `rect rgb(r, g, b)` blocks to highlight critical security validation, rate limiting, or threat detection zones.
4. **Branching (`alt / else / opt`)**: Explicitly document both success (`200 OK`, `201 Created`) and failure paths (`400 Bad Request`, `401 Unauthorized`, `403 Forbidden`, `429 Too Many Requests`).
5. **No Data Loss**: Show the exact moment logs are written to `logs/smart-bussing.json` before any exception terminates execution.

---

## EXAMPLES

### Example 1: DLP & Content Moderation Flow (`POST /api/v1/reporteRuta/{id_ruta}`)

```mermaid
sequenceDiagram
    autonumber
    actor Cliente as App Móvil / Postman
    participant Filter as SecurityAuditFilter (HTTP)
    participant Controller as ReporteRutaController
    participant Service as ReporteRutaService
    participant DLP as DlpValidationService (DataFog)
    participant Log as logs/smart-bussing.json (Wazuh)
    participant DB as PostgreSQL

    Cliente->>Filter: POST /api/v1/reporteRuta/1
    Note over Filter: Inyecta IP, URI y aplica Rate Limit en MDC
    Filter->>Controller: Pasa petición limpia
    Controller->>Service: createNewReporteRuta(dto, rutaId, email)
    
    rect rgb(255, 235, 235)
    Note over Service,DLP: 1. Valida si la ruta está bloqueada
    Note over Service,DLP: 2. Pasa primero por DataFog DLP
    Service->>DLP: validateComment(descripcion, rutaId, email)
    end

    alt Contenido Obsceno o Fuga PII detectada
        DLP->>Log: Escribe JSON con threat_type="OBSCENE_CONTENT"
        DLP-->>Service: Lanza DlpViolationException (Corta la ejecución)
        Note over DB: NUNCA se ejecuta reporteRutaRepository.save()
        Service-->>Cliente: HTTP 400 Bad Request (ApiResponse)
    else Comentario Limpio
        DLP-->>Service: Aprobado
        Service->>DB: reporteRutaRepository.save(newReporte)
        DB-->>Cliente: HTTP 201 Created
    end
```

### Example 2: Authentication & Account Lockout Flow (`POST /api/v1/user/login`)

```mermaid
sequenceDiagram
    autonumber
    actor Cliente as Usuario / Atacante
    participant Filter as SecurityAuditFilter
    participant RateLimiter as RateLimiterService
    participant Controller as UsuarioController
    participant Service as UsuarioService
    participant Log as logs/smart-bussing.json (Wazuh)
    participant DB as PostgreSQL

    Cliente->>Filter: POST /api/v1/user/login
    Filter->>RateLimiter: isAllowed(IP, URI)
    alt Rate Limit Excedido (> 5 req/min)
        RateLimiter-->>Filter: Denegado
        Filter->>Log: Alerta RATE_LIMIT_EXCEEDED (HTTP 429)
        Filter-->>Cliente: HTTP 429 Too Many Requests
    else Rate Limit Permitido
        Filter->>Controller: Pasa petición
        Controller->>Service: login(email, password)
        Service->>DB: findByEmail(email)
        alt Cuenta Bloqueada por 5 intentos fallidos
            Service->>Log: Evento resource_forbidden (HTTP 403)
            Service-->>Cliente: HTTP 403 Forbidden (Cuenta bloqueada 15m)
        else Credenciales Inválidas
            Service->>Service: Incrementar contador fallos
            Service->>Log: Evento auth_failure (HTTP 401)
            Service-->>Cliente: HTTP 401 Unauthorized
        else Credenciales Correctas
            Service->>Service: Resetear contador fallos
            Service-->>Cliente: HTTP 200 OK (Token / UsuarioDTO)
        end
    end
```
