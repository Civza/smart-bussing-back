# SmartBussingMobile - Guía de Implementación y Patrones (CLAUDE)

Este documento centraliza los patrones de diseño, convenciones de nombres e implementaciones de referencia para que el asistente de IA los use al generar código nuevo (especialmente con el comando `/new-feature`).

## Comandos Personalizados
- `/load-project`: Lee CLAUDE.md, SPEC.md y WORKLOG.md en ese orden. El asistente debe responder con un resumen del estado actual, qué estaba en progreso y el próximo paso. No debe escribir código hasta recibir aprobación.
- `/review-code`: Revisa el código recién escrito con mentalidad de senior developer estricto. Evalúa: 1) Bugs/Edge cases, 2) Cumplimiento de CLAUDE.md, 3) Simplificación (over-engineering), 4) Manejo de errores, 5) Riesgos en producción. El asistente será directo y claro.
- `/new-feature [descripción]`: Inicia la implementación de una nueva feature. Primero genera un plan para describir el approach. Al ser aprobado, implementa en orden: Backend (modelo → repo → servicio → controller → ruta) y luego Frontend (tipos → hook → componente → integración). Escribe tipos TS primero y usa patrones de REFERENCES.md.
- `/fix-bug [descripción]`: Diagnostica y arregla un bug. El asistente responderá con: 1) Causa probable, 2) Archivos involucrados, y 3) El fix mínimo necesario. Promete estrictamente NO arreglar nada más allá del bug reportado y mostrar el diff exacto de los cambios a realizar.
- `/update-worklog`: Actualiza WORKLOG.md al terminar la sesión. Incluye: Fecha, Qué implementamos (lista de lo hecho), Qué quedó en progreso, Bloqueos y Próximos pasos por prioridad.
- `/add-reference [tema]`: Agrega a REFERENCES.md un snippet de ejemplo para un nuevo patrón o librería. Incluye: Cuándo usar el patrón, snippet mínimo funcional y gotchas/detalles importantes, manteniendo el estilo existente.
- `/update-spec [cambio]`: Actualiza SPEC.md ante cambios de diseño o alcance. Asegura: 1) Marcar [x] features implementadas, 2) Agregar nuevas con [ ], 3) Actualizar modelo de datos, 4) Actualizar endpoints.
- `/agent [rol]`: Invoca un sub-agente experto para revisar código bajo un enfoque específico:
  - `security`: Busca SQL injection, mala autenticación, secrets, inputs no sanitizados y JWT mal configurado. Reporta con severidad (CRÍTICO/ALTO/MEDIO/BAJO).
  - `dba`: DBA experto en PostgreSQL. Revisa queries eficientes/índices, normalización, N+1 queries, foreign keys y constraints.
  - `qa`: QA Engineer. Genera casos de prueba necesarios, edge cases probables y un test de integración (happy path) en el stack del proyecto (Vitest/Jest).
  - `frontend`: Experto en React-native/UX. Evalúa manejo de estado, re-renders innecesarios, UX completa (loading/error/empty) y cohesión del componente (si debe dividirse).

## 1. Stack Tecnológico

| Tecnología | Versión | Rol |
|---|---|---|
| Java | 17 | Lenguaje principal |
| Spring Boot | 3.4.5 | Framework base |
| Spring Data JPA | — | ORM / acceso a datos |
| Spring WebFlux | — | Cliente HTTP reactivo (llamadas a Mapbox) |
| PostgreSQL | — | Base de datos |
| Lombok | — | Reducción de boilerplate en entidades |
| JGraphT | 1.5.2 | Algoritmo de rutas óptimas (grafo de paradas) |
| Maven | — | Build tool |
| Docker | — | Containerización |

---

## 2. Estructura del Proyecto

```
smart-bussing-back/
├── docs/
│   ├── CLAUDE.MD                        # Este archivo
│   └── add_new_route_instrucctions.md   # Guía GeoJSON para rutas y paradas
├── src/main/java/buss/smartbussingapi/
│   ├── SmartbussingApiApplication.java  # Entry point
│   ├── CorsConfig.java                  # Configuración CORS global
│   │
│   ├── commons/                         # Utilidades transversales
│   │   ├── ApiResponse.java             # Record genérico de respuesta { info, response, error }
│   │   ├── GlobalControllerAdvice.java  # Manejo global de excepciones (@RestControllerAdvice)
│   │   ├── Methods.java                 # Helpers/utilidades compartidas
│   │   └── exceptions/
│   │       ├── NotFoundException.java
│   │       ├── AlreadyExistsException.java
│   │       ├── InvalidDataException.java
│   │       └── InvalidCredentialsException.java
│   │
│   ├── DTOs/                            # Objetos de transferencia entre capas/módulos
│   │   ├── UsuarioDTO.java
│   │   ├── ReporteRutaDTO.java
│   │   ├── DirectionsResponse.java      # Respuesta de Mapbox Directions API
│   │   ├── RegistroLugarDTO.java        # DTO para registro de lugar + empresa
│   │   ├── RegistroLugarMapper.java     # Mapper: DTO → Empresa + Lugar
│   │   ├── RegistroLugarController.java # Controller registro lugar (POST /api/v1/registrarLugar)
│   │   ├── GeoJsonRoute/                # DTOs para importar rutas desde GeoJSON
│   │   ├── GeoJsonStops/                # DTOs para importar paradas desde GeoJSON
│   │   └── ItineraryDTOs/              # DTOs de respuesta del itinerario de viaje
│   │
│   ├── Ruta/                            # Módulo de rutas de bus
│   ├── Parada/                          # Módulo de paradas
│   ├── Coordenadas/                     # Módulo de coordenadas GPS
│   ├── Viaje/                           # Módulo de viajes / cálculo de itinerario
│   │   └── CreatingOptimistPath/        # Algoritmo de ruta óptima
│   ├── Usuario/                         # Módulo de usuarios
│   ├── Lugar/                           # Módulo de lugares (destinos)
│   ├── Empresa/                         # Módulo de empresas
│   ├── Contrato/                        # Módulo de contratos
│   ├── Images/                          # Módulo de imágenes
│   ├── Interesado/                      # Módulo de interesados
│   └── Reporte/                         # Módulo de reportes de rutas
```

---

## 3. Patrones de Diseño Arquitectónico

### 3.1 Arquitectura por módulos (package-by-feature)

Cada dominio vive en su propio paquete con sus 4 archivos canónicos:

```
<Modulo>/
├── <Modulo>.java           # Entidad JPA (@Entity)
├── <Modulo>Repository.java # Repositorio JPA (extends JpaRepository)
├── <Modulo>Service.java    # Lógica de negocio
└── <Modulo>Controller.java # Endpoints REST (@RestController)
```

### 3.2 Wrapper de respuesta universal — `ApiResponse<T>`

**Todos** los endpoints retornan este record:
```java
public record ApiResponse<T>(String info, T response, String error) {}
```

| Campo | Uso |
|---|---|
| `info` | Mensaje descriptivo de la operación |
| `response` | Payload de datos (null en errores) |
| `error` | Mensaje de error (null en éxito) |

**Ejemplo:**
```java
return new ApiResponse<>("Route created", ruta, null);        // éxito
return new ApiResponse<>("Not found", null, e.getMessage());  // error (manejado por GlobalControllerAdvice)
```

### 3.3 Manejo centralizado de excepciones

`GlobalControllerAdvice` intercepta y mapea las excepciones del dominio a HTTP codes:

| Excepción | HTTP Status |
|---|---|
| `NotFoundException` | 404 Not Found |
| `InvalidDataException` | 400 Bad Request |
| `AlreadyExistsException` | 409 Conflict |
| `InvalidCredentialsException` | 401 Unauthorized |
| `RuntimeException` (genérica) | 500 Internal Server Error |

**Patrón de uso en Service:**
```java
throw new NotFoundException("Ruta con id " + id + " no encontrada");
```

### 3.4 Ingesta de datos geográficos vía GeoJSON

Rutas y paradas se crean mediante DTOs que parsean el formato GeoJSON estándar. El `Service` convierte el DTO en entidades JPA. Ver `add_new_route_instrucctions.md` para el formato completo.

### 3.5 Cálculo de ruta óptima (`Viaje/CreatingOptimistPath`)

| Clase | Rol |
|---|---|
| `GraphBuilderService` | Construye un grafo de paradas con JGraphT |
| `AlgoService` | Ejecuta el algoritmo de camino más corto |
| `MapboxService` | Llama a Mapbox Directions API (WebClient reactivo) |
| `BuildBusGeoJson` | Construye la respuesta GeoJSON del itinerario |

---

## 4. Convenciones de Nombres

### Paquetes y clases
- **Paquetes de módulo**: `PascalCase` — `Ruta`, `Parada`, `Usuario`
- **Entidades**: `PascalCase` igual al paquete — `Ruta.java`
- **Repositorios**: `<Entidad>Repository` — `RutaRepository`
- **Servicios**: `<Entidad>Service` o `<Entidad>sService` — `RutaService`, `ViajesService`
- **Controllers**: `<Entidad>Controller` — `RutaController`
- **DTOs**: sufijo `DTO` — `GeoJsonRouteDTO`, `ItineraryResponseDTO`
- **Excepciones**: sufijo `Exception` — `NotFoundException`

### Campos de entidades JPA
- Usar `snake_case` para campos que mapean directo a columna — `id_ruta`, `nombre_ruta`
- Las relaciones se nombran en plural — `paradas`, `coordenadas`, `reporteRutas`

### Variables locales y parámetros
- `camelCase` estándar Java — `id_ruta` como `@PathVariable` es tolerado por convención del proyecto

### Rutas REST
- Base: `/api/v1/<recurso>` en singular o plural según convención del módulo
- Path variables: `{id_<entidad>}` — `/api/v1/ruta/{id_ruta}`

---

## 5. Endpoints por Módulo

### Usuario — `api/v1/user`
| Método | Ruta | Descripción |
|---|---|---|
| GET | `/api/v1/user` | Listar todos los usuarios |
| GET | `/api/v1/user/{id_user}` | Obtener usuario por ID |
| GET | `/api/v1/user/email?email=` | Obtener usuario por email (retorna UsuarioDTO) |
| POST | `/api/v1/user` | Registrar nuevo usuario |
| POST | `/api/v1/user/login?email=&password=` | Login (retorna Usuario completo o 401) |
| PATCH | `/api/v1/user/mn/{id_user}?name=` | Actualizar nombre |
| PATCH | `/api/v1/user/mp/{id_user}?password=` | Actualizar contraseña |

### Ruta — `/api/v1/ruta`
| Método | Ruta | Descripción |
|---|---|---|
| GET | `/api/v1/ruta` | Listar todas las rutas |
| GET | `/api/v1/ruta/{id_ruta}` | Obtener ruta por ID |
| GET | `/api/v1/ruta/coordenadas/{id_ruta}` | Obtener coordenadas de una ruta |
| POST | `/api/v1/ruta` | Crear ruta desde GeoJSON (`GeoJsonRouteDTO`) |

### Parada — `api/v1/parada`
| Método | Ruta | Descripción |
|---|---|---|
| GET | `api/v1/parada` | Listar todas las paradas |
| GET | `api/v1/parada/{id_parada}` | Obtener parada por ID |
| POST | `api/v1/parada` | Crear parada desde GeoJSON (`GeoJsonStopDTO`) |

### Viaje — `api/v1/viaje`
| Método | Ruta | Descripción |
|---|---|---|
| GET | `api/v1/viaje` | Listar todos los viajes |
| GET | `api/v1/viaje/{id_viaje}` | Obtener viaje por ID |
| POST | `api/v1/viaje` | Crear viaje (en desarrollo) |
| GET | `api/v1/viaje/draft/get-travel?userLat=&userLon=&destLat=&destLon=` | Calcular itinerario óptimo |
| GET | `api/v1/viaje/get-walking-to-stop?userLat=&userLon=` | Caminar a parada más cercana (test) |

### Lugar — `api/v1/lugar`
| Método | Ruta | Descripción |
|---|---|---|
| GET | `api/v1/lugar` | Listar todos los lugares |
| GET | `api/v1/lugar/{id_lugar}` | Obtener lugar por ID |

### Reporte de Ruta — `/api/v1/reporteRuta`
| Método | Ruta | Descripción |
|---|---|---|
| GET | `/api/v1/reporteRuta` | Listar todos los reportes |
| GET | `/api/v1/reporteRuta/{reporte_ruta_id}` | Obtener reporte por ID |
| GET | `/api/v1/reporteRuta/byRuta?routeName=` | Obtener reportes por nombre de ruta |
| POST | `/api/v1/reporteRuta/{id_ruta}?email=` | Crear nuevo reporte para una ruta |

### Registro de Lugar — `api/v1/registrarLugar`
| Método | Ruta | Descripción |
|---|---|---|
| POST | `api/v1/registrarLugar` | Registrar lugar + empresa juntos (`RegistroLugarDTO`) |

---

## 6. Modelo de Datos (Relaciones clave)

```
Ruta ──ManyToMany──> Coordenadas  (tabla pivot: coordenadas_ruta)
Ruta ──ManyToMany──> Parada       (tabla pivot: ruta_parada)
Ruta ──OneToMany───> ReporteRuta
Usuario ──(lógica)──> Viaje
Empresa ──(lógica)──> Lugar
```

### Entidad `Ruta`
| Campo | Tipo | Descripción |
|---|---|---|
| `id_ruta` | int (PK, SEQUENCE) | Identificador único |
| `nombre_ruta` | String | Nombre largo de la ruta |
| `nombre_corto_ruta` | String | Nombre corto/número |
| `color_ruta` | String | Color en hex (#RRGGBB) |
| `color_texto_ruta` | String | Color de texto en hex |
| `tipo_ruta` | String | Tipo de vehículo (microbus, etc.) |
| `horario_ruta` | String | Horario de operación |
| `active` | boolean | Si la ruta está activa |
| `coordenadas` | List\<Coordenadas\> | ManyToMany |
| `paradas` | List\<Parada\> | ManyToMany |
| `reporteRutas` | List\<ReporteRuta\> | OneToMany (@JsonIgnore) |

---

## 7. Variables de Entorno (`.env`)

El proyecto requiere las siguientes variables (ver `.env` en la raíz):
- Conexión a PostgreSQL (URL, usuario, contraseña)
- Token de Mapbox API (para `MapboxService`)

> ⚠️ **Nunca commitear el `.env` al repositorio.** Está en `.gitignore`.

