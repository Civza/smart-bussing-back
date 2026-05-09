# WORKLOG — Issue #9: Registro de Lugar

**Fecha:** 2026-05-08
**Branch:** `Issue#9`
**Autor:** Emiliano

---

## ¿Qué se implementó?

Se refinó el flujo de registro unificado de Lugar + Empresa a través del endpoint `POST /api/v1/registrarLugar`. Los cambios se concentran en el DTO y el Mapper.

### Archivos modificados

| Archivo | Cambio |
|---|---|
| `RegistroLugarDTO.java` | Simplificado: se comenta `paisEmpresa` (hardcodeado en mapper), se comenta bloque de contrato (`fechaInicio`, `fechaFin`, `monto`); se agrega `List<String> urlFiles` |
| `RegistroLugarMapper.java` | `toEmpresa()` busca primero por email y reutiliza empresa existente (evita duplicados); se agregan validaciones guard clause; `toContrato()` comentado; `toLugar()` mapea `urlFiles` y `direccion`, `tipo` hardcodeado a `"Indefinido"` |

### Decisiones de diseño

- **Idempotencia de empresa**: Si ya existe una empresa con ese correo, se usa la existente en lugar de lanzar error o crear duplicado. Esto permite registrar múltiples lugares bajo la misma empresa.
- **Contrato diferido**: La lógica de contratos (`fechaInicio`, `fechaFin`, `monto`) queda comentada hasta que se defina el flujo de negocio completo.
- **`tipo` hardcodeado**: El campo `tipo` del lugar se setea como `"Indefinido"` por ahora, hasta que el frontend envíe valores reales.

---

## ⚠️ Limitaciones / TODOs

- [ ] Definir y activar la lógica de `Contrato` (fechas y monto).
- [ ] El campo `tipo` del `Lugar` debería recibirse en el DTO, no hardcodearse.
- [ ] `paisEmpresa` hardcodeado a `"Mexico"` — parametrizar cuando se soporte multi-país.
- [ ] Las validaciones en el Mapper lanzan `IllegalArgumentException` en lugar de `InvalidDataException` del dominio. Migrar para consistencia con `GlobalControllerAdvice`.

---

# WORKLOG — Issue #17: Draft Route Endpoint (Itinerario Básico)

**Fecha:** 2026-05-07
**Branch:** `Issue#17`
**Autor:** Emiliano

---

## ¿Qué se implementó?

Se implementó el endpoint `GET /api/v1/viaje/draft/get-travel` que calcula y devuelve un itinerario de viaje en bus desde la ubicación del usuario hasta su destino. El resultado es una lista ordenada de **segmentos** (`WALKING` o `BUS`), cada uno con su ruta GeoJSON, instrucciones y métricas de distancia/tiempo.

### Flujo implementado

```
Usuario (lat, lon) ──► findNearestStop ──► ¿está cerca? ──► WALKING seg. 1
                                                │
                                         A* (AlgoService)
                                                │
                                      lista de Paradas óptimas
                                                │
                                    BuildBusGeoJson (LineString)
                                                │
                                           BUS segment
                                                │
                              findNearestStop (destino) ──► ¿está lejos? ──► WALKING seg. 2
                                                │
                                    ItineraryResponseDTO → Frontend
```

### Archivos modificados / creados

| Archivo | Cambio |
|---|---|
| `ViajesService.java` | Implementación completa del flujo `getDraftRoute` (Steps 1–5) |
| `MapboxService.java` | Refactor + 2 sobrecargas de `getWalkingDirections`, `callMapboxWalking` extraído, manejo de errores HTTP, `ObjectMapper` estático |
| `BuildBusGeoJson.java` | Elimina `ruta_id` hardcodeado → deriva la ruta común automáticamente con `findCommonRoute`; guard clause para lista vacía |
| `AlgoService.java` | Elimina `haversine` duplicado → usa `commons.Methods.haversine` |
| `Parada.java` | Agrega relación `@ManyToMany(mappedBy="paradas")` con `Ruta` + `@JsonIgnore` para que `findCommonRoute` funcione |
| `docs/CLAUDE.MD` | Documentación arquitectónica completa del proyecto |

---

## ⚠️ Estado del algoritmo — MUY SIMPLE (v0.1)

> El algoritmo de rutas implementado en este issue es **una primera versión funcional mínima**. Cubre únicamente el caso más básico y tiene múltiples limitaciones conocidas que deberán resolverse en issues futuros.

### Limitaciones actuales

- **Una sola ruta por trayecto:** `findCommonRoute` busca la primera ruta que compartan la parada de origen y la de destino. Si no existe ninguna ruta común, lanza `NotFoundException`. No hay fallback.
- **Sin transbordos:** Si el usuario necesita cambiar de ruta a mitad del camino, el sistema falla. No existe lógica de multi-segmento en bus.
- **Sin tiempo real:** `totalSeconds` y `totalMeters` se acumulan solo con los segmentos de caminata. El tiempo del segmento de bus **no se calcula** (marcado con `TODO`).
- **Sin predicción de tiempos de bus:** Los tiempos de espera en parada no existen. No hay integración con GTFS ni horarios reales.
- **Grafo estático:** `GraphBuilderService` reconstruye el grafo completo en cada request desde la BD. No hay caché.
- **Parada más cercana ≠ parada más útil:** `findNearestStop` usa solo distancia Haversine. No considera si esa parada tiene rutas que vayan en la dirección correcta.

---

## 📋 TO-DOs para el próximo issue

### 🔴 Crítico — funcionalidad core

- [ ] **Soporte de transbordos:** Implementar lógica de multi-ruta en `AlgoService`/`ViajesService`. El itinerario debe poder tener más de un segmento `BUS` con paradas de transbordo entre ellos.
- [ ] **Cálculo real de tiempo de bus:** Integrar distancia del segmento GeoJSON de bus en `totalMeters` y estimar `totalSeconds` (velocidad promedio de bus o GTFS si está disponible).
- [ ] **`findNearestStop` con filtro de dirección:** La parada más cercana debería ser la más cercana *que tenga una ruta útil hacia el destino*, no solo la geográficamente más próxima.

### 🟠 Importante — robustez

- [ ] **Caché del grafo en `GraphBuilderService`:** El grafo no cambia frecuentemente. Usar `@Cacheable` de Spring o construirlo una sola vez al arrancar el contexto (`@PostConstruct`) en lugar de por cada request.
- [ ] **Manejo de `path == null` en `ViajesService`:** Actualmente `AlgoService` retorna lista vacía si A* no encuentra ruta, pero `BuildBusGeoJson` lanzará `InvalidDataException`. El `ViajesService` debería capturar este caso y responder con un mensaje claro de "No se encontró ruta entre los puntos indicados".
- [ ] **Validación de coordenadas en el endpoint:** Verificar que `userLat`, `userLon`, `destLat`, `destLon` estén dentro de rangos válidos antes de llamar a Mapbox.

### 🟡 Mejoras — calidad y escalabilidad

- [ ] **Tiempo de espera en parada:** Agregar un campo `tiempoEsperaEstimadoSegundos` en `SegmentoResponseDTO` para el segmento `BUS`.
- [ ] **`paradaMap` en `AlgoService`:** Se hace un `findAll()` de paradas en cada llamada. Considerar pasar el mapa como parámetro desde `ViajesService` o cachearlo.
- [ ] **Tests de integración:** Cubrir el happy path del endpoint `get-travel` con paradas y rutas en una BD H2 en memoria.
- [ ] **Separar `getDraftRoute` en pasos más pequeños:** El método tiene 60+ líneas. Extraer Steps 1–2 a `buildWalkToStopSegment()` y Steps 4–5 a `buildBusSegment()` / `buildWalkToDestSegment()`.

---

## Próximo issue sugerido

**Issue #18 — Transbordos y tiempo real en el itinerario**
Implementar soporte de múltiples rutas de bus en un solo itinerario (transbordos), calcular el tiempo real del segmento de bus y cachear el grafo de paradas.
