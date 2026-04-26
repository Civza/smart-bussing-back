# GeoJSON Structure - Smart Bussing

## Overview

GeoJSON is a format for encoding geographic data structures using JSON. The smart bussing system uses GeoJSON to represent bus routes as LineString geometries and bus stops as Point geometries, with associated properties for identification and styling.

## Basic Structure

The root object is a FeatureCollection containing an array of Feature objects.

### FeatureCollection

| Property | Type | Description |
|----------|------|-------------|
| `type` | String | Always `"FeatureCollection"` |
| `features` | Array | Array of Feature objects (routes and stops) |

**Example:**
```json
{
  "type": "FeatureCollection",
  "features": [...]
}
```

---

## Feature Types

Each feature has a `type`, `id`, `geometry`, and `properties`. Two types are used in smart bussing:

### Routes (LineString)

- **Purpose**: Represents the path a bus takes through the city
- **Geometry**: Array of `[longitude, latitude]` coordinates in order
- **Properties**: Color, type, and route identification

**Example Route:**
```json
{
  "type": "Feature",
  "id": "route-101",
  "geometry": {
    "type": "LineString",
    "coordinates": [
      [-116.6060, 31.8665],
      [-116.6080, 31.8620],
      [-116.6040, 31.8560],
      [-116.5980, 31.8490],
      [-116.5935, 31.8385]
    ]
  },
  "properties": {
    "feature_type": "route",
    "route_id": "101",
    "route_short_name": "101",
    "route_long_name": "Centro - Reforma - UABC",
    "route_color": "#34D399",
    "route_text_color": "#065F46",
    "route_type": "microbus"
  }
}
```

### Stops (Point)

- **Purpose**: Represents a bus stop location
- **Geometry**: Single `[longitude, latitude]` coordinate pair
- **Properties**: Stop identification and linked routes

**Example Stop:**
```json
{
  "type": "Feature",
  "id": "stop-ST001",
  "geometry": {
    "type": "Point",
    "coordinates": [-116.6060, 31.8665]
  },
  "properties": {
    "feature_type": "stop",
    "stop_id": "ST001",
    "stop_name": "Parada Centro",
    "stop_description": "Centro histórico de Ensenada",
    "routes": ["101", "103"]
  }
}
```

---

## Route Properties

| Property | Type | Example | Required | Description |
|----------|------|---------|----------|-------------|
| `feature_type` | String | `"route"` | Yes | Identifies this as a route feature |
| `route_id` | String | `"101"` | Yes | Unique identifier for the route |
| `route_short_name` | String | `"101"` | Yes | Short display name (usually route number) |
| `route_long_name` | String | `"Centro - Reforma - UABC"` | Yes | Full descriptive name of the route |
| `route_color` | Hex String | `"#34D399"` | Yes | Display color in hex format (without # optional) |
| `route_text_color` | Hex String | `"#065F46"` | Yes | Text color in hex format for contrast |
| `route_type` | String | `"microbus"` | Yes | Type of transport vehicle |

---

## Stop Properties

| Property | Type | Example | Required | Description |
|----------|------|---------|----------|-------------|
| `feature_type` | String | `"stop"` | Yes | Identifies this as a stop feature |
| `stop_id` | String | `"ST001"` | Yes | Unique identifier for the stop |
| `stop_name` | String | `"Parada Centro"` | Yes | Display name of the stop |
| `stop_description` | String | `"Centro histórico de Ensenada"` | No | Detailed description or address |
| `routes` | Array | `["101", "103"]` | Yes | Array of route IDs that serve this stop |

---

## Database Mapping

How GeoJSON properties map to JPA entity fields:

| GeoJSON Property | Entity | Database Field | Type |
|------------------|--------|-----------------|------|
| `longitude` | Coordenadas | `longitud` | Double |
| `latitude` | Coordenadas | `latitud` | Double |
| `route_id` | Ruta | `routeShortName` | String |
| `route_long_name` | Ruta | `routeLongName` | String |
| `route_color` | Ruta | `routeColor` | String |
| `route_text_color` | Ruta | `routeTextColor` | String |
| `route_type` | Ruta | `routeType` | String |
| `stop_id` | Parada | `idParada` | Integer |
| `stop_name` | Parada | `nombreParada` | String |
| `stop_description` | Parada | `stopDescription` | String |
| `routes` | Parada | `rutas` | List<Ruta> (ManyToMany) |

---

## Coordinate System

- **Standard**: WGS84 (EPSG:4326) - used globally by all mapping systems
- **Order**: `[longitude, latitude]` ⚠️ **NOT** latitude, longitude
- **Range**:
    - Longitude: -180 to 180
    - Latitude: -90 to 90
- **Ensenada Location**: Approximately `[-116.6°, 31.8°]`

### Common Mistake

```json
// ❌ WRONG - latitude first
"coordinates": [31.8665, -116.6060]

// ✅ CORRECT - longitude first
"coordinates": [-116.6060, 31.8665]
```

---

## Color Format

Colors are specified as hexadecimal values in web format:

| Format | Example | Usage |
|--------|---------|-------|
| 6-digit hex | `#34D399` | Preferred (RGB) |
| 3-digit hex | `#3D9` | Shorthand (expands to `#33DD99`) |
| RGB values | `(52, 211, 153)` | For database storage alternative |

### Color Examples for Routes

| Route | Color | Text Color | Purpose |
|-------|-------|-----------|---------|
| Route 101 | `#34D399` (Green) | `#065F46` (Dark Green) | Standard visibility |
| Route 102 | `#F59E0B` (Amber) | `#78350F` (Dark Brown) | Warm tone contrast |
| Route 103 | `#818CF8` (Indigo) | `#1E1B4B` (Dark Blue) | Cool tone contrast |

---

## Complete Example: Route 101

**Route 101: Centro - Reforma - UABC**

```json
{
  "type": "Feature",
  "id": "route-101",
  "geometry": {
    "type": "LineString",
    "coordinates": [
      [-116.6060, 31.8665],
      [-116.6080, 31.8620],
      [-116.6040, 31.8560],
      [-116.5980, 31.8490],
      [-116.5935, 31.8385]
    ]
  },
  "properties": {
    "feature_type": "route",
    "route_id": "101",
    "route_short_name": "101",
    "route_long_name": "Centro - Reforma - UABC",
    "route_color": "#34D399",
    "route_text_color": "#065F46",
    "route_type": "microbus"
  }
}
```

**Stops served by Route 101:**

| Stop ID | Stop Name | Coordinates | Description |
|---------|-----------|-------------|-------------|
| ST001 | Parada Centro | [-116.6060, 31.8665] | Centro histórico de Ensenada |
| ST005 | Ampliación Indeco | [-116.6080, 31.8620] | Colonia Ampliación Indeco |
| ST002 | UABC Valle Dorado | [-116.5935, 31.8385] | Universidad Autónoma de Baja California |

---

## Data Validation Rules

When adding new routes or stops, ensure:

### Routes

- [ ] `route_id` is unique and alphanumeric
- [ ] `route_short_name` is concise (typically 1-3 characters)
- [ ] `route_long_name` is descriptive (25-50 characters recommended)
- [ ] `route_color` and `route_text_color` have sufficient contrast (WCAG AA standard)
- [ ] All coordinates are valid longitude/latitude pairs within Ensenada bounds
- [ ] Route follows a logical geographic path with smooth transitions
- [ ] `route_type` matches available vehicle types (e.g., "microbus", "bus", "metro")

### Stops

- [ ] `stop_id` is unique and follows naming convention (e.g., "ST001", "ST002")
- [ ] `stop_name` is clear and identifies the location
- [ ] `stop_description` provides context (optional but recommended)
- [ ] Coordinates match actual physical stop locations
- [ ] `routes` array contains only valid `route_id` values that actually serve this stop
- [ ] Stop coordinates fall within reasonable distance from route path

---

## Best Practices

1. **Coordinate Precision**
    - Store coordinates as Double in database for GPS precision
    - Use at least 4 decimal places for accuracy (~11 meters in Ensenada)
    - Example: `-116.6060` (4 decimals) ✓ vs `-116.606` (3 decimals) ✗

2. **Color Accessibility**
    - Ensure `route_color` and `route_text_color` meet WCAG AA contrast ratio (4.5:1)
    - Test colors with accessibility tools (e.g., WebAIM Contrast Checker)
    - Avoid red/green only distinction for colorblind users

3. **Route Organization**
    - Coordinate order should follow actual bus direction (start to end)
    - Include intermediate stops to ensure accuracy
    - Keep waypoint density consistent (~50-200 meters apart)

4. **Stop Management**
    - Always link stops to routes that actually serve them
    - Keep descriptions concise and informative
    - Use consistent naming (e.g., "Parada Centro" not "Centro Stop" or "CENTRO")
    - Verify coordinates are on or near actual roads

5. **Documentation**
    - Update this document when adding new routes
    - Include route opening/closing dates if applicable
    - Document any route changes or modifications
    - Keep a changelog of updates

---

## Template: Adding a New Route

Copy this template when adding a new route:

```json
{
  "type": "Feature",
  "id": "route-XXX",
  "geometry": {
    "type": "LineString",
    "coordinates": [
      [LONGITUDE, LATITUDE],
      [LONGITUDE, LATITUDE],
      [LONGITUDE, LATITUDE]
    ]
  },
  "properties": {
    "feature_type": "route",
    "route_id": "XXX",
    "route_short_name": "XXX",
    "route_long_name": "START POINT - INTERMEDIATE - END POINT",
    "route_color": "#RRGGBB",
    "route_text_color": "#RRGGBB",
    "route_type": "microbus"
  }
}
```

**Steps:**
1. Replace `XXX` with unique route number
2. Add 4-5 key waypoint coordinates following the route
3. Choose a distinct color that contrasts with text color
4. Describe the route from start to end destination
5. Validate all coordinates are within Ensenada bounds
6. Test rendering on map before adding to production

---

## Template: Adding a New Stop

Copy this template when adding a new stop:

```json
{
  "type": "Feature",
  "id": "stop-STXXX",
  "geometry": {
    "type": "Point",
    "coordinates": [LONGITUDE, LATITUDE]
  },
  "properties": {
    "feature_type": "stop",
    "stop_id": "STXXX",
    "stop_name": "STOP NAME",
    "stop_description": "Description of area or landmark",
    "routes": ["route_id_1", "route_id_2"]
  }
}
```

**Steps:**
1. Replace `XXX` with unique stop number
2. Enter accurate GPS coordinates of physical stop
3. Use clear, recognizable stop name
4. Add description for tourist/new rider context
5. List only routes that actually serve this stop
6. Verify stop is actually on or near the route path

---

## Common Issues & Troubleshooting

| Issue | Cause | Solution |
|-------|-------|----------|
| Routes not displaying on map | Invalid coordinate format (lat/lon instead of lon/lat) | Verify all coordinates are `[longitude, latitude]` |
| Routes appear broken or incomplete | Coordinates too far apart or skipping area | Add intermediate waypoints for smoother path |
| Text unreadable on map | Poor color contrast | Use contrast checker; ensure 4.5:1 minimum ratio |
| Stop not on route | Stop coordinates don't align with route | Verify GPS coordinates; adjust to nearest road point |
| Stops linked to wrong routes | Incorrect `routes` array | Cross-reference each stop with actual route maps |
| Database load errors | String coordinates instead of Double | Convert all coordinates from String to Double type |


## Resources

- **GeoJSON Specification**: [RFC 7946](https://tools.ietf.org/html/rfc7946)
- **WGS84 Coordinate System**: [EPSG:4326](https://epsg.io/4326)
- **Leaflet Map Library**: [leafletjs.com](https://leafletjs.com)
- **Color Contrast Tool**: [WebAIM Contrast Checker](https://webaim.org/resources/contrastchecker/)
- **GPS Coordinate Validator**: [GPS Visualizer](https://www.gpsvisualizer.com/calculators)

---

**Last Updated**: 2025-04-24  
**Version**: 1.0  
**Maintainer**: Smart Bussing Team