# Smart Bussing Ensenada - Backend API & Guía de Seguridad con Wazuh y DataFog DLP

Repositorio oficial del backend REST de **Smart Bussing Ensenada** (Spring Boot 3.4.5, Java 17, PostgreSQL). Este proyecto provee los servicios para el mapeo, enrutamiento óptimo (algoritmo A* y JGraphT), reportes ciudadanos y gestión de usuarios para el transporte público de Ensenada, B.C.

---

## 1. Análisis del Documento de Ciberseguridad (CETYS Universidad)

Basado en el informe de ciberseguridad empresarial del proyecto **Smart Bussing**, se identificaron los siguientes aspectos críticos:

### Activos Clave Protegidos
1. **Base de Datos PostgreSQL (Crítica):** Integridad de perfiles, credenciales, capas GeoJSON de rutas y paradas, e historial de reportes.
2. **API REST Backend (Alta):** Disponibilidad de la lógica de enrutamiento y autenticación; su caída deja inoperable la app móvil y el portal web.
3. **Ubicación GPS en Tiempo Real (Alta):** Datos personales regulados bajo la **LFPDPPP**; requieren protección en tránsito.
4. **Credenciales y Sesiones (Alta):** Control de acceso contra ataques de fuerza bruta y secuestro de sesiones.
5. **API Keys de Terceros (Mapbox SDK / Stripe) (Media-Alta):** Mitigación de abusos que agoten cuotas o generen sobrecostos.
6. **Integridad de Comentarios y Prevención de Fuga de Datos (Alta):** Mitigación de spam, doxxing, fuga de PII y lenguaje ofensivo en reportes comunitarios públicos.

### Políticas de Seguridad Implementadas en este Backend
- **Política 1 (Sanitización y Control de Entrada):** Consultas parametrizadas con JPA y filtros tempranos de inspección contra inyecciones SQL y caracteres sospechosos.
- **Política 2 (Cifrado y Cabeceras de Seguridad):** Inyección de cabeceras de seguridad HTTP (`HSTS`, `X-Content-Type-Options`, `X-Frame-Options`, `X-XSS-Protection`) para forzar TLS y mitigar ataques Man-in-the-Middle y clickjacking.
- **Política 3 (Resiliencia y Monitoreo SIEM):** Trazabilidad estructurada de eventos de seguridad y control de tasa (Rate Limiting) exportados en JSON hacia **Wazuh SIEM**.
- **Política 4 (DLP & Moderación de Contenido con DataFog Core):** Inspección de reportes de usuarios (`POST /api/v1/reporteRuta/{id_ruta}`) para bloquear y alertar comentarios con lenguaje obsceno o fuga de datos personales (PII), complementado con escaneo de secretos vía **Gitleaks**.

---

## 2. Mejoras de Solidificación del Backend Implementadas

### A. Respuestas Semánticas y Recursos Bloqueados (HTTP 403 / 429 en lugar de 200 sin datos)
- **Recursos Inactivos o Bloqueados (`403 Forbidden`):**
  - Si un cliente intenta consultar una ruta inactiva/bloqueada (`/api/v1/ruta/{id}`) o sus coordenadas (`/api/v1/ruta/coordenadas/{id}`), o crear reportes sobre ella, el sistema retorna **403 Forbidden** con `ResourceBlockedException` y un mensaje claro, en vez de un 200 vacío o datos inconsistentes.
- **Protección contra Fuerza Bruta en Autenticación (`403 Forbidden` tras 5 intentos fallidos):**
  - Tras 5 intentos consecutivos con credenciales erróneas en `/api/v1/user/login`, la cuenta se bloquea temporalmente por 15 minutos respondiendo con **403 Forbidden** y generando una alerta de seguridad para Wazuh.
- **Corrección de Endpoints que retornaban 200 sin cuerpo:**
  - El endpoint `/api/v1/registrarLugar` (`POST`) valida la información y retorna un `ApiResponse` estructurado con estado **200 OK / 201 Created**.

### B. Control de Tasa de Peticiones (`RateLimiterService` - HTTP 429)
Implementación en memoria de ventana deslizante por dirección IP para evitar DoS, fuerza bruta y agotamiento de cuota de APIs externas (Mapbox):
- **Límite General de la API:** 100 peticiones / minuto por IP.
- **Límite Estricto en Login (`/api/v1/user/login`):** 5 peticiones / minuto por IP.
- **Límite en Rutas y Tránsito (`/api/v1/viaje`):** 30 peticiones / minuto por IP.
- Respuesta al exceder el límite: **HTTP 429 Too Many Requests** con cabecera `Retry-After: 60`.

### C. Detección Temprana de Amenazas (`SecurityAuditFilter`)
El filtro intercepta las solicitudes antes de llegar a los controladores e inspecciona la URI y los parámetros:
- **Detección de SQL Injection:** Patrones típicos (`UNION SELECT`, `' OR '1'='1`, `DROP TABLE`, `--`).
- **Detección de Path Traversal:** Secuencias `../`, `%2e%2e%2f`, etc.
- **Detección de XSS Básico:** `<script>`, `javascript:`, tags maliciosos.
- Al detectar cualquier ataque, aborta la petición inmediatamente con **403 Forbidden** y escribe un registro estructurado con `threat_type` y nivel `ERROR`.

### D. DLP & Moderación de Contenido Ciudadano (`DlpValidationService` & `dlp-service`)
En la creación de reportes ciudadanos (`POST /api/v1/reporteRuta/{id_ruta}`):
- Se analiza el contenido del comentario (`descripcion`) mediante el microservicio complementario de **DataFog Core** y moderación de contenido.
- Si contiene lenguaje ofensivo o fuga de datos personales no autorizados (PII), se bloquea la solicitud retornando **HTTP 400 Bad Request** con `DlpViolationException`.
- Se genera un registro estructurado en MDC con `threat_type: "OBSCENE_CONTENT"` o `"DLP_PII_LEAK"` hacia `logs/smart-bussing.json` para alertar en Wazuh.
- Posee mecanismo de respaldo local automático si el microservicio DataFog estuviese temporalmente fuera de línea.

### E. Cabeceras de Seguridad Inyectadas
En cada respuesta se inyectan automáticamente:
```http
X-Content-Type-Options: nosniff
X-Frame-Options: DENY
X-XSS-Protection: 1; mode=block
Strict-Transport-Security: max-age=31536000; includeSubDomains
Referrer-Policy: strict-origin-when-cross-origin
```

---

## 3. Logs Estructurados para Wazuh

La aplicación genera registros en formato **JSON estructurado** en:
```
logs/smart-bussing.json
```
A través de `logback-spring.xml` y `LogstashEncoder`, cada evento incluye variables contextuales (MDC):

| Campo JSON | Descripción | Ejemplo |
|---|---|---|
| `client_ip` | IP origen del cliente (soporta proxies `X-Forwarded-For`) | `192.168.1.100` |
| `http_method` | Método HTTP | `POST`, `GET` |
| `request_uri` | URI solicitada | `/api/v1/reporteRuta/2` |
| `query_params` | Parámetros de consulta | `email=admin@test.com` |
| `http_status` | Código HTTP resultante | `200`, `400`, `401`, `403`, `429`, `500` |
| `event_type` | Tipo de evento | `security_alert`, `auth_failure`, `resource_forbidden`, `rate_limit_exceeded` |
| `threat_type` | Tipo de amenaza detectada | `SQL_INJECTION`, `PATH_TRAVERSAL`, `OBSCENE_CONTENT`, `DLP_PII_LEAK` |
| `route_id` | Identificador de la ruta asociada al reporte | `2` |
| `user_email` | Correo del usuario emisor del evento | `usuario@cetys.mx` |
| `message` | Mensaje descriptivo del evento | `Alerta de seguridad: Intento de ataque/violacion detectado...` |

---

## 4. Instrucciones de Configuración para Wazuh (Copy & Paste)

### Paso 1: Configurar la Ingesta de Logs en el Agente Wazuh (`ossec.conf`)
En el archivo de configuración del agente Wazuh:
```xml
<!-- Ingesta de logs JSON del backend de Smart Bussing -->
<localfile>
  <log_format>json</log_format>
  <location>/ruta/a/smart-bussing-back/logs/smart-bussing.json</location>
</localfile>
```

---

### Paso 2: Decodificadores en Wazuh (`/var/ossec/etc/decoders/local_decoder.xml`)
```xml
<!-- Decodificador para Smart Bussing API Logs -->
<decoder name="smartbussing-json">
  <prematch>buss\.smartbussingapi</prematch>
</decoder>
```

---

### Paso 3: Reglas de Detección en Wazuh (`/var/ossec/etc/rules/local_rules.xml`)
Copia y pega las siguientes reglas dentro de `<group name="smartbussing,web,">` en `/var/ossec/etc/rules/local_rules.xml`:

```xml
<group name="smartbussing,web,">

  <!-- 100100: Regla Base para la API de Smart Bussing -->
  <rule id="100100" level="0">
    <field name="logger_name">^buss\.smartbussingapi</field>
    <description>Evento generado por Smart Bussing API</description>
  </rule>

  <!-- 100101: Acceso Web Exitoso (HTTP 200 / 201) -->
  <rule id="100101" level="3">
    <if_sid>100100</if_sid>
    <field name="event_type">^access_success$</field>
    <description>Smart Bussing: Petición procesada exitosamente [$(json.http_method) $(json.request_uri)]</description>
  </rule>

  <!-- 100102: Fallo de Autenticación (HTTP 401) -->
  <rule id="100102" level="5">
    <if_sid>100100</if_sid>
    <field name="http_status">^401$</field>
    <field name="event_type">^auth_failure$</field>
    <description>Smart Bussing: Fallo de autenticación / Credenciales inválidas desde IP $(json.client_ip)</description>
    <group>authentication_failed,pci_dss_10.2.4,pci_dss_10.2.5,</group>
  </rule>

  <!-- 100103: Ataque de Fuerza Bruta en Login (5 fallos en 2 minutos) -->
  <rule id="100103" level="10" frequency="5" timeframe="120">
    <if_matched_sid>100102</if_matched_sid>
    <same_field>client_ip</same_field>
    <description>Smart Bussing ALERTA: Múltiples fallos de autenticación desde IP $(json.client_ip) (Posible ataque de Fuerza Bruta)</description>
    <mitre>
      <id>T1110</id>
    </mitre>
    <group>authentication_failures,</group>
  </rule>

  <!-- 100104: Recurso Bloqueado / Acceso Denegado (HTTP 403) -->
  <rule id="100104" level="7">
    <if_sid>100100</if_sid>
    <field name="http_status">^403$</field>
    <field name="event_type">^resource_forbidden$</field>
    <description>Smart Bussing: Acceso denegado a recurso inactivo o bloqueado [$(json.request_uri)] desde IP $(json.client_ip)</description>
    <group>access_denied,</group>
  </rule>

  <!-- 100105: Tasa de Peticiones Excedida / Rate Limit (HTTP 429) -->
  <rule id="100105" level="8">
    <if_sid>100100</if_sid>
    <field name="http_status">^429$</field>
    <field name="event_type">^rate_limit_exceeded$</field>
    <description>Smart Bussing ALERTA: Rate limit excedido desde IP $(json.client_ip) en URI $(json.request_uri)</description>
    <mitre>
      <id>T1499</id>
    </mitre>
    <group>dos_attack,</group>
  </rule>

  <!-- 100106: Intento de Inyección SQL Detectado (Crítico) -->
  <rule id="100106" level="12">
    <if_sid>100100</if_sid>
    <field name="threat_type">^SQL_INJECTION$</field>
    <description>Smart Bussing ALERTA CRÍTICA: Intento de SQL Injection bloqueado desde IP $(json.client_ip) en $(json.request_uri)</description>
    <mitre>
      <id>T1190</id>
    </mitre>
    <group>sqli,attack,</group>
  </rule>

  <!-- 100107: Intento de Path Traversal Detectado (Crítico) -->
  <rule id="100107" level="12">
    <if_sid>100100</if_sid>
    <field name="threat_type">^PATH_TRAVERSAL$</field>
    <description>Smart Bussing ALERTA CRÍTICA: Intento de Path Traversal bloqueado desde IP $(json.client_ip) en $(json.request_uri)</description>
    <mitre>
      <id>T1083</id>
    </mitre>
    <group>path_traversal,attack,</group>
  </rule>

  <!-- 100108: Intento de Cross-Site Scripting (XSS) Detectado -->
  <rule id="100108" level="10">
    <if_sid>100100</if_sid>
    <field name="threat_type">^XSS_ATTACK$</field>
    <description>Smart Bussing ALERTA: Intento de ataque XSS bloqueado desde IP $(json.client_ip) en $(json.request_uri)</description>
    <mitre>
      <id>T1059.007</id>
    </mitre>
    <group>xss,attack,</group>
  </rule>

  <!-- 100109: Error Interno del Servidor (HTTP 500) -->
  <rule id="100109" level="5">
    <if_sid>100100</if_sid>
    <field name="http_status">^50[0-9]$</field>
    <description>Smart Bussing: Error interno del servidor (HTTP $(json.http_status)) en $(json.request_uri)</description>
  </rule>

  <!-- 100110: Violación de DLP / Contenido Obsceno en Comentarios (DataFog Core) -->
  <rule id="100110" level="10">
    <if_sid>100100</if_sid>
    <field name="threat_type">^OBSCENE_CONTENT|DLP_PII_LEAK$</field>
    <description>Smart Bussing ALERTA DLP: Contenido obsceno o fuga de datos detectada en comentario por DataFog desde IP $(json.client_ip) [Ruta $(json.route_id), Usuario: $(json.user_email)]</description>
    <mitre>
      <id>T1059</id>
    </mitre>
    <group>dlp,content_moderation,datafog,</group>
  </rule>

  <!-- 100111: Reincidencia o Flood de Comentarios Ofensivos (3 intentos en 120s) -->
  <rule id="100111" level="12" frequency="3" timeframe="120">
    <if_matched_sid>100110</if_matched_sid>
    <same_field>client_ip</same_field>
    <description>Smart Bussing ALERTA CRÍTICA: Múltiples intentos repetidos de envío de comentarios ofensivos desde IP $(json.client_ip)</description>
    <group>dlp,abuse,flood,</group>
  </rule>

</group>
```

---

## 5. Verificación de las Reglas con `wazuh-logtest`

En tu servidor Wazuh, ejecuta:
```bash
/var/ossec/bin/wazuh-logtest
```
Y pega los siguientes ejemplos generados por el backend para comprobar el match de cada regla:

#### Prueba 1: Detección de Inyección SQL (Regla 100106 - Nivel 12)
```json
{"@timestamp":"2026-10-25T14:30:00.000Z","level":"ERROR","logger_name":"buss.smartbussingapi.SecurityAuditFilter","client_ip":"192.168.1.50","http_method":"GET","request_uri":"/api/v1/user","query_params":"email=' OR '1'='1","event_type":"security_alert","threat_type":"SQL_INJECTION","http_status":"403","message":"Alerta de seguridad: Intento de ataque detectado [SQL_INJECTION] desde IP 192.168.1.50"}
```

#### Prueba 2: Recurso Bloqueado / Acceso Denegado (Regla 100104 - Nivel 7)
```json
{"@timestamp":"2026-10-25T14:30:00.000Z","level":"WARN","logger_name":"buss.smartbussingapi.SecurityAuditFilter","client_ip":"192.168.1.60","http_method":"GET","request_uri":"/api/v1/ruta/12","event_type":"resource_forbidden","http_status":"403","message":"Acceso denegado: Recurso bloqueado o permisos insuficientes"}
```

#### Prueba 3: Rate Limiting Excedido (Regla 100105 - Nivel 8)
```json
{"@timestamp":"2026-10-25T14:30:00.000Z","level":"WARN","logger_name":"buss.smartbussingapi.SecurityAuditFilter","client_ip":"192.168.1.70","http_method":"POST","request_uri":"/api/v1/user/login","event_type":"rate_limit_exceeded","threat_type":"RATE_LIMIT_EXCEEDED","http_status":"429","message":"Alerta de seguridad: Tasa de solicitudes excedida (Rate Limit)"}
```

#### Prueba 4: Fallo de Autenticación (Regla 100102 - Nivel 5)
```json
{"@timestamp":"2026-10-25T14:30:00.000Z","level":"WARN","logger_name":"buss.smartbussingapi.SecurityAuditFilter","client_ip":"192.168.1.80","http_method":"POST","request_uri":"/api/v1/user/login","event_type":"auth_failure","http_status":"401","message":"Fallo de autenticacion: Credenciales invalidas o no proporcionadas"}
```

#### Prueba 5: Violación DLP / Contenido Obsceno detectado por DataFog (Regla 100110 - Nivel 10)
```json
{"@timestamp":"2026-11-03T18:30:00.000Z","level":"ERROR","logger_name":"buss.smartbussingapi.Reporte.DlpValidationService","client_ip":"192.168.1.95","http_method":"POST","request_uri":"/api/v1/reporteRuta/2","event_type":"security_alert","threat_type":"OBSCENE_CONTENT","route_id":"2","user_email":"usuario@cetys.mx","http_status":"400","message":"Alerta de seguridad: Intento de ataque/violacion detectado [OBSCENE_CONTENT] en reporte de ruta 2"}
```

---

## 6. Ejecución y Compilación del Proyecto

### Iniciar el Microservicio Sidecar DLP (DataFog)
```bash
# Opción A: Con Python local
cd dlp-service
pip install -r requirements.txt
uvicorn main:app --port 8000

# Opción B: Con Docker Compose
docker compose -f docker-compose-dlp.yml up -d
```

### Pruebas Unitarias del Backend (Spring Boot)
```bash
# Ejecutar todas las pruebas unitarias (incluyendo DlpValidationServiceTest)
./mvnw test -Dtest="!*IntegrationTest,!SmartbussingApiApplicationTests"
```

### Ejecución en Desarrollo del Backend
```bash
./mvnw spring-boot:run
```
Los logs se escribirán simultáneamente en consola y en `logs/smart-bussing.json`.
