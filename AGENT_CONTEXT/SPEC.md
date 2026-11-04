# Project Specifications (SPEC.md)

This document is the single source of truth for requirements, architecture, and system
design decisions for **Smart Bussing API**.

## Overview
**Smart Bussing API** — Backend high-performance service for public urban transit in Ensenada, B.C., featuring multimodal trip planning (bus lines + pedestrian legs), route topologies, stop management, and citizen incident reporting.

## System Architecture
- **Structure**: Separate repositories (`SmartBussingMobile` and `smart-bussing-back`).
- **Frontend**: React Native 0.81 + Expo SDK 54 (`SmartBussingMobile`).
- **Backend**: Java 17 / Spring Boot 3.4.5 (`spring-boot-starter-web`, `spring-boot-starter-data-jpa`).
- **Database**: PostgreSQL hosted on cloud (Railway / Render) with HikariCP connection pooling.
- **Routing Engine**: JGraphT (`org.jgrapht:jgrapht-core`) for graph-based multimodal pathfinding + Mapbox Directions API for pedestrian segments.

## Solution Layers
1. **Routing & Transit Engine**:
   - `GraphBuilderService`: Transforms bus routes and stops into a graph of `RouteVertex` nodes and edges.
   - `AlgoService`: Calculates optimal shortest paths across the transit network using JGraphT.
   - `PathSegmenterService`: Segments transit itineraries into `WALKING`, `BUS`, and `TRANSFER` legs.
   - `MapboxService`: Integrates with Mapbox Directions API for walking instructions.
2. **Core Domain Entities**: Routes (`Ruta`), Coordinates (`Coordenadas`), Stops (`Parada`), Operators (`Empresa`), Contracts (`Contrato`).
3. **User & Auth Layer**: Citizen user accounts (`Usuario`) and authentication.
4. **Citizen Reporting & Community**: Route feedback, issue reports (`ReporteRuta`), and likes.

## Requirements and Implementation Status (Milestones)

### Phase 1: Planning and Design
- [x] Scope definition and domain model design.
- [x] Database schema for routes, coordinates, and stops.
- [x] CORS and application configuration.

### Phase 2: Base Development & Routing Engine
- [x] JGraphT graph builder and optimal path solver (`AlgoService`).
- [x] Integration with Mapbox Directions for pedestrian walking segments.
- [x] Multimodal route drafting endpoint (`GET /api/v1/viaje/draft/get-travel`).
- [x] CRUD controllers for routes, stops, and coordinates.
- [x] Comprehensive test suites added (`RutaServiceTest`, `UsuarioControllerTest`, `GraphBuilderServiceTest`).

### Phase 3: Auth Security & Real-Time Enhancements
- [ ] Refactor `/api/v1/user/login` to consume JSON body instead of URL query parameters.
- [ ] Real-time bus telemetry ingestion (MQTT / WebSockets).
- [ ] Push notification service for transit alerts.

## API Endpoints Summary (`/api/v1`)
| Method | Endpoint | Description |
|---|---|---|
| GET | `/viaje/draft/get-travel` | Generates optimal multimodal route between origin & destination |
| GET | `/ruta` | Lists all active bus routes |
| GET | `/ruta/{id}` | Retrieves route details and geometry |
| POST | `/ruta` | Imports route from GeoJSON FeatureCollection |
| GET | `/parada` | Lists official bus stops |
| POST | `/parada` | Creates a new bus stop GeoJSON |
| POST | `/user/login` | Authenticates user credentials |
| POST | `/user` | Registers a new user |
| GET | `/reporteRuta` | Retrieves citizen reports and comments per route |
| POST | `/reporteRuta/{id_ruta}` | Creates a new incident report |

## Testing
- Unit and integration tests with JUnit 5, Mockito, and Spring Boot Test (`mvn test`).
