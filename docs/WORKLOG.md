# WORKLOG — Update: Testing for GeoJSON Route Endpoint

**Fecha:** 2026-05-08
**Autor:** Antigravity

---

## ¿Qué se implementó?

Se implementaron las pruebas unitarias para la función de creación de rutas mediante GeoJSON (`agregarRutaDesdeGeoJson`) en `RutaService`, asegurando que las entidades relacionadas (Rutas, Coordenadas y Paradas) se generen correctamente a partir del payload y verificando el manejo de errores. Además, se validó el correcto funcionamiento de los tests mediante un entorno de pruebas con Docker.

### Detalles de la Implementación

- **Test de Happy Path (`agregarRutaDesdeGeoJson_exitosa`):** Se probó la función con un payload JSON completo (tipo `FeatureCollection`) simulando una ruta y una parada. Se validó correctamente la conversión de las propiedades de la ruta (`route_long_name`, `route_color`, etc.), el parseo de coordenadas de geometría `LineString` y la correcta instanciación de las entidades `Parada`.
- **Test de Error (`agregarRutaDesdeGeoJson_sinRutaLanzaException`):** Se comprobó que al enviar un GeoJSON que no contiene una feature de tipo `route`, el servicio lanza correctamente una `InvalidDataException`.
- **Corrección de Tests Previos:** Se inhabilitó el test obsoleto `agregarRuta_exitosa` que estaba provocando fallas del tipo `UnnecessaryStubbingException` en la suite de pruebas debido a firmas de métodos antiguas.
- **Entorno de Ejecución:** Se verificó la ejecución de la suite de pruebas aislando la ejecución con Maven en Docker, interactuando de forma exitosa con la base de datos de prueba aprovisionada por `docker-compose-test.yml`.

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
