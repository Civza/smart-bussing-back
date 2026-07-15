package buss.smartbussingapi.Ruta;

import buss.smartbussingapi.DTOs.RutaRequestDTO;
import buss.smartbussingapi.DTOs.GeoJsonFeatureCollectionDTO;
import buss.smartbussingapi.commons.exceptions.InvalidDataException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
@ActiveProfiles("test")
@ExtendWith(MockitoExtension.class)
public class RutaServiceTest {

    @Mock
    RutaRepository rutaRepository;

    @Mock
    buss.smartbussingapi.Viaje.CreatingOptimistPath.GraphBuilderService graphBuilderService;

    @InjectMocks
    RutaService rutaService;

    private Ruta buildMockSavedRuta(int id_ruta, String nombre_ruta, String nombre_corto_ruta, String color_ruta, String color_texto_ruta, RutaType tipo_ruta, String horario_ruta, boolean active) {
        var ruta = new Ruta(id_ruta, nombre_ruta, nombre_corto_ruta, color_ruta, color_texto_ruta, tipo_ruta, horario_ruta, active, true, null, null, null);
        return ruta;
    }
    
    /*
    @Test
    void agregarRuta_exitosa() {

        var request = new RutaRequestDTO("Ruta 1", "R1", "red", "white", "LineString", "10:00-18:00", true);
        var mockSavedRuta = buildMockSavedRuta(1, "Ruta 1", "R1", "red", "white", "LineString", "10:00-18:00", true);

        when(rutaRepository.save(any())).thenReturn(mockSavedRuta);

        var response = rutaService.agregarRuta(request);

        verify(rutaRepository, times(1)).save(any());
        assertEquals(1, response.getId_ruta());
        assertEquals("Ruta 1", response.getNombre_ruta());
        assertEquals("R1", response.getNombre_corto_ruta());
        assertEquals("red", response.getColor_ruta());
        assertEquals("white", response.getColor_texto_ruta());
        assertEquals("LineString", response.getTipo_ruta());
        assertEquals("10:00-18:00", response.getHorario_ruta());
        assertTrue(response.isActive());

    }
    */

    @Test
    void agregarRutaDesdeGeoJson_exitosa() throws Exception {
        String json = """
            {
              "type": "FeatureCollection",
              "features": [
                {
                  "type": "Feature",
                  "properties": {
                    "feature_type": "route",
                    "route_long_name": "Ruta de prueba",
                    "route_short_name": "RP",
                    "route_color": "#FF0000",
                    "route_text_color": "#FFFFFF",
                    "route_type": "MICROBUS"
                  },
                  "geometry": {
                    "type": "LineString",
                    "coordinates": [
                      [-116.59, 31.86],
                      [-116.60, 31.87]
                    ]
                  }
                },
                {
                  "type": "Feature",
                  "properties": {
                    "feature_type": "stop",
                    "stop_name": "Parada 1",
                    "stop_description": "Primera parada"
                  },
                  "geometry": {
                    "type": "Point",
                    "coordinates": [-116.595, 31.865]
                  }
                }
              ]
            }
            """;
            
        ObjectMapper mapper = new ObjectMapper();
        GeoJsonFeatureCollectionDTO payload = mapper.readValue(json, GeoJsonFeatureCollectionDTO.class);
        
        when(rutaRepository.save(any(Ruta.class))).thenAnswer(i -> i.getArguments()[0]);
        
        Ruta result = rutaService.agregarRutaDesdeGeoJson(payload);
        
        assertNotNull(result);
        assertEquals("Ruta de prueba", result.getNombreRuta());
        assertEquals("RP", result.getNombreCortoRuta());
        assertEquals("#FF0000", result.getColorRuta());
        assertEquals("#FFFFFF", result.getColorTextoRuta());
        assertEquals(RutaType.MICROBUS, result.getTipoRuta());
        assertTrue(result.isActive());
        
        assertNotNull(result.getCoordenadas());
        assertEquals(2, result.getCoordenadas().size());
        assertEquals(-116.59, result.getCoordenadas().get(0).getLongitud());
        assertEquals(31.86, result.getCoordenadas().get(0).getLatitud());
        assertEquals(-116.60, result.getCoordenadas().get(1).getLongitud());
        assertEquals(31.87, result.getCoordenadas().get(1).getLatitud());
        
        assertNotNull(result.getParadas());
        assertEquals(1, result.getParadas().size());
        assertEquals("Parada 1", result.getParadas().get(0).getNombreParada());
        assertEquals("Primera parada", result.getParadas().get(0).getDescripcionParada());
        assertNotNull(result.getParadas().get(0).getCoordenadasParada());
        assertEquals(-116.595, result.getParadas().get(0).getCoordenadasParada().getLongitud());
        assertEquals(31.865, result.getParadas().get(0).getCoordenadasParada().getLatitud());
        
        verify(rutaRepository, times(1)).save(any(Ruta.class));
    }

    @Test
    void agregarRutaDesdeGeoJson_sinRutaLanzaException() throws Exception {
        String json = """
            {
              "type": "FeatureCollection",
              "features": [
                {
                  "type": "Feature",
                  "properties": {
                    "feature_type": "stop",
                    "stop_name": "Parada 1"
                  },
                  "geometry": {
                    "type": "Point",
                    "coordinates": [-116.595, 31.865]
                  }
                }
              ]
            }
            """;
            
        ObjectMapper mapper = new ObjectMapper();
        GeoJsonFeatureCollectionDTO payload = mapper.readValue(json, GeoJsonFeatureCollectionDTO.class);
        
        assertThrows(InvalidDataException.class, () -> {
            rutaService.agregarRutaDesdeGeoJson(payload);
        });
    }

    @Test
    void getRutaById_Success() {
        Ruta r = new Ruta();
        r.setIdRuta(1);
        r.setNombreRuta("Test Ruta");
        when(rutaRepository.findById(1)).thenReturn(java.util.Optional.of(r));
        Ruta result = rutaService.getRutaById(1);
        assertNotNull(result);
        assertEquals("Test Ruta", result.getNombreRuta());
    }

    @Test
    void getRutaById_NotFound() {
        when(rutaRepository.findById(1)).thenReturn(java.util.Optional.empty());
        assertThrows(buss.smartbussingapi.commons.exceptions.NotFoundException.class, () -> rutaService.getRutaById(1));
    }

    @Test
    void getAllRutas() {
        Ruta r = new Ruta();
        when(rutaRepository.findAll()).thenReturn(java.util.List.of(r));
        assertEquals(1, rutaService.getAllRutas().size());
    }

    @Test
    void getCoordenadasRuta_Success() {
        Ruta r = new Ruta();
        r.setIdRuta(1);
        buss.smartbussingapi.Coordenadas.Coordenadas c = new buss.smartbussingapi.Coordenadas.Coordenadas();
        c.setLatitud(1.0);
        c.setLongitud(1.0);
        r.setCoordenadas(java.util.List.of(c));
        when(rutaRepository.findById(1)).thenReturn(java.util.Optional.of(r));
        java.util.List<buss.smartbussingapi.Coordenadas.Coordenadas> coords = rutaService.getCoordenadasRuta(1);
        assertEquals(1, coords.size());
        assertEquals(1.0, coords.get(0).getLatitud());
    }

    @Test
    void getCoordenadasRuta_notFound() {
        when(rutaRepository.findById(1)).thenReturn(java.util.Optional.empty());
        assertThrows(buss.smartbussingapi.commons.exceptions.NotFoundException.class, () -> rutaService.getCoordenadasRuta(1));
    }

    @Test
    void getCoordenadasRuta_emptyCoords() {
        Ruta r = new Ruta();
        r.setIdRuta(1);
        r.setCoordenadas(new java.util.ArrayList<>());
        when(rutaRepository.findById(1)).thenReturn(java.util.Optional.of(r));
        assertThrows(buss.smartbussingapi.commons.exceptions.NotFoundException.class, () -> rutaService.getCoordenadasRuta(1));
    }

    @Test
    void createNewRouteFromGeoJSON_invalidLatitude() throws Exception {
        String json = """
            {
              "type": "Feature",
              "properties": {
                "feature_type": "route"
              },
              "geometry": {
                "type": "LineString",
                "coordinates": [
                  [-116.59, 95.0]
                ]
              }
            }
            """;
        ObjectMapper mapper = new ObjectMapper();
        buss.smartbussingapi.DTOs.GeoJsonRoute.GeoJsonRouteDTO payload = mapper.readValue(json, buss.smartbussingapi.DTOs.GeoJsonRoute.GeoJsonRouteDTO.class);
        
        assertThrows(InvalidDataException.class, () -> rutaService.createNewRouteFromGeoJSON(payload));
    }

    @Test
    void agregarRutaDesdeGeoJson_nullPayload() {
        assertThrows(InvalidDataException.class, () -> rutaService.agregarRutaDesdeGeoJson(null));
    }

    @Test
    void agregarRutaDesdeGeoJson_customSentidoAndBidirectionalFalse() throws Exception {
        String json = """
            {
              "type": "FeatureCollection",
              "features": [
                {
                  "type": "Feature",
                  "properties": {
                    "feature_type": "route",
                    "bidirectional": false
                  },
                  "geometry": {
                    "type": "LineString",
                    "coordinates": [
                      [-116.59, 31.86, "IDA"]
                    ]
                  }
                }
              ]
            }
            """;
        ObjectMapper mapper = new ObjectMapper();
        GeoJsonFeatureCollectionDTO payload = mapper.readValue(json, GeoJsonFeatureCollectionDTO.class);
        
        when(rutaRepository.save(any(Ruta.class))).thenAnswer(i -> i.getArguments()[0]);
        
        Ruta result = rutaService.agregarRutaDesdeGeoJson(payload);
        assertNotNull(result);
        assertEquals("IDA", result.getCoordenadas().get(0).getSentido());
    }

    @Test
    void createNewRouteFromGeoJSON_MissingRouteType_And_LatLonCoverage() throws Exception {
        String json = """
            {
              "type": "Feature",
              "properties": {
                "feature_type": "route"
              },
              "geometry": {
                "type": "LineString",
                "coordinates": [
                  [-181.0, 0.0]
                ]
              }
            }
            """;
        ObjectMapper mapper = new ObjectMapper();
        buss.smartbussingapi.DTOs.GeoJsonRoute.GeoJsonRouteDTO payload1 = mapper.readValue(json, buss.smartbussingapi.DTOs.GeoJsonRoute.GeoJsonRouteDTO.class);
        assertThrows(InvalidDataException.class, () -> rutaService.createNewRouteFromGeoJSON(payload1));

        String json2 = """
            {
              "type": "Feature",
              "properties": {
                "feature_type": "route"
              },
              "geometry": {
                "type": "LineString",
                "coordinates": [
                  [181.0, 0.0]
                ]
              }
            }
            """;
        buss.smartbussingapi.DTOs.GeoJsonRoute.GeoJsonRouteDTO payload2 = mapper.readValue(json2, buss.smartbussingapi.DTOs.GeoJsonRoute.GeoJsonRouteDTO.class);
        assertThrows(InvalidDataException.class, () -> rutaService.createNewRouteFromGeoJSON(payload2));

        String json3 = """
            {
              "type": "Feature",
              "properties": {
                "feature_type": "route"
              },
              "geometry": {
                "type": "LineString",
                "coordinates": [
                  [0.0, -91.0]
                ]
              }
            }
            """;
        buss.smartbussingapi.DTOs.GeoJsonRoute.GeoJsonRouteDTO payload3 = mapper.readValue(json3, buss.smartbussingapi.DTOs.GeoJsonRoute.GeoJsonRouteDTO.class);
        assertThrows(InvalidDataException.class, () -> rutaService.createNewRouteFromGeoJSON(payload3));

        String json4 = """
            {
              "type": "Feature",
              "properties": {
                "feature_type": "route"
              },
              "geometry": {
                "type": "LineString",
                "coordinates": [
                  [0.0, 0.0]
                ]
              }
            }
            """;
        buss.smartbussingapi.DTOs.GeoJsonRoute.GeoJsonRouteDTO payload4 = mapper.readValue(json4, buss.smartbussingapi.DTOs.GeoJsonRoute.GeoJsonRouteDTO.class);
        when(rutaRepository.save(any(Ruta.class))).thenAnswer(i -> i.getArguments()[0]);
        Ruta res = rutaService.createNewRouteFromGeoJSON(payload4);
        assertEquals(RutaType.URBANA, res.getTipoRuta());
    }

    @Test
    void agregarRutaDesdeGeoJson_MissingPropertiesBranches() throws Exception {
        String json = """
            {
              "type": "FeatureCollection",
              "features": [
                {
                  "type": "Feature",
                  "properties": {
                    "feature_type": "route"
                  },
                  "geometry": {
                    "type": "LineString",
                    "coordinates": [
                      [-116.59, 31.86]
                    ]
                  }
                },
                {
                  "type": "Feature",
                  "properties": {
                    "feature_type": "stop"
                  }
                },
                {
                  "type": "Feature"
                }
              ]
            }
            """;
        ObjectMapper mapper = new ObjectMapper();
        GeoJsonFeatureCollectionDTO payload = mapper.readValue(json, GeoJsonFeatureCollectionDTO.class);
        
        when(rutaRepository.save(any(Ruta.class))).thenAnswer(i -> i.getArguments()[0]);
        Ruta res = rutaService.agregarRutaDesdeGeoJson(payload);
        
        assertEquals("", res.getNombreRuta());
        assertEquals("", res.getNombreCortoRuta());
        assertEquals("#000000", res.getColorRuta());
        assertEquals("#FFFFFF", res.getColorTextoRuta());
        assertEquals(RutaType.URBANA, res.getTipoRuta());
        assertTrue(res.isBidirectional());
        assertEquals("AMBOS", res.getCoordenadas().get(0).getSentido());
    }

    @Test
    void createNewRouteFromGeoJSON_invalidFeatureType() throws Exception {
        String json = """
            {
              "type": "Feature",
              "properties": {
                "feature_type": "not-route"
              },
              "geometry": {
                "type": "LineString",
                "coordinates": [[0.0, 0.0]]
              }
            }
            """;
        ObjectMapper mapper = new ObjectMapper();
        buss.smartbussingapi.DTOs.GeoJsonRoute.GeoJsonRouteDTO payload = mapper.readValue(json, buss.smartbussingapi.DTOs.GeoJsonRoute.GeoJsonRouteDTO.class);
        assertThrows(InvalidDataException.class, () -> rutaService.createNewRouteFromGeoJSON(payload));
    }

    @Test
    void createNewRouteFromGeoJSON_invalidGeometryType() throws Exception {
        String json = """
            {
              "type": "Feature",
              "properties": {
                "feature_type": "route"
              },
              "geometry": {
                "type": "Point",
                "coordinates": [0.0, 0.0]
              }
            }
            """;
        ObjectMapper mapper = new ObjectMapper();
        buss.smartbussingapi.DTOs.GeoJsonRoute.GeoJsonRouteDTO payload = mapper.readValue(json, buss.smartbussingapi.DTOs.GeoJsonRoute.GeoJsonRouteDTO.class);
        assertThrows(InvalidDataException.class, () -> rutaService.createNewRouteFromGeoJSON(payload));
    }

    @Test
    void createNewRouteFromGeoJSON_nullOrEmptyCoordinates() throws Exception {
        String jsonNull = """
            {
              "type": "Feature",
              "properties": {
                "feature_type": "route"
              },
              "geometry": {
                "type": "LineString"
              }
            }
            """;
        String jsonEmpty = """
            {
              "type": "Feature",
              "properties": {
                "feature_type": "route"
              },
              "geometry": {
                "type": "LineString",
                "coordinates": []
              }
            }
            """;
        ObjectMapper mapper = new ObjectMapper();
        buss.smartbussingapi.DTOs.GeoJsonRoute.GeoJsonRouteDTO payloadNull = mapper.readValue(jsonNull, buss.smartbussingapi.DTOs.GeoJsonRoute.GeoJsonRouteDTO.class);
        buss.smartbussingapi.DTOs.GeoJsonRoute.GeoJsonRouteDTO payloadEmpty = mapper.readValue(jsonEmpty, buss.smartbussingapi.DTOs.GeoJsonRoute.GeoJsonRouteDTO.class);
        
        assertThrows(InvalidDataException.class, () -> rutaService.createNewRouteFromGeoJSON(payloadNull));
        assertThrows(InvalidDataException.class, () -> rutaService.createNewRouteFromGeoJSON(payloadEmpty));
    }

    @Test
    void createNewRouteFromGeoJSON_boundaryCoordinates() throws Exception {
        String json = """
            {
              "type": "Feature",
              "properties": {
                "feature_type": "route"
              },
              "geometry": {
                "type": "LineString",
                "coordinates": [
                  [-180.0, -90.0],
                  [180.0, 90.0]
                ]
              }
            }
            """;
        ObjectMapper mapper = new ObjectMapper();
        buss.smartbussingapi.DTOs.GeoJsonRoute.GeoJsonRouteDTO payload = mapper.readValue(json, buss.smartbussingapi.DTOs.GeoJsonRoute.GeoJsonRouteDTO.class);
        when(rutaRepository.save(any(Ruta.class))).thenAnswer(i -> i.getArguments()[0]);
        Ruta res = rutaService.createNewRouteFromGeoJSON(payload);
        assertNotNull(res);
        assertEquals(-180.0, res.getCoordenadas().get(0).getLongitud());
        assertEquals(-90.0, res.getCoordenadas().get(0).getLatitud());
        assertEquals(180.0, res.getCoordenadas().get(1).getLongitud());
        assertEquals(90.0, res.getCoordenadas().get(1).getLatitud());
    }

    @Test
    void agregarRutaDesdeGeoJson_nullFeatures() {
        GeoJsonFeatureCollectionDTO payload = new GeoJsonFeatureCollectionDTO("FeatureCollection", null);
        assertThrows(InvalidDataException.class, () -> rutaService.agregarRutaDesdeGeoJson(payload));
    }

    @Test
    void agregarRutaDesdeGeoJson_nullRouteGeometryAndNonArrayCoords() throws Exception {
        String json = """
            {
              "type": "FeatureCollection",
              "features": [
                {
                  "type": "Feature",
                  "properties": {
                    "feature_type": "route"
                  }
                }
              ]
            }
            """;
        ObjectMapper mapper = new ObjectMapper();
        GeoJsonFeatureCollectionDTO payload = mapper.readValue(json, GeoJsonFeatureCollectionDTO.class);
        when(rutaRepository.save(any(Ruta.class))).thenAnswer(i -> i.getArguments()[0]);
        Ruta res = rutaService.agregarRutaDesdeGeoJson(payload);
        assertNotNull(res);
        assertTrue(res.getCoordenadas().isEmpty());
    }

    @Test
    void agregarRutaDesdeGeoJson_routeGeomNoCoordsAndNotArray() throws Exception {
        String jsonNoCoords = """
            {
              "type": "FeatureCollection",
              "features": [
                {
                  "type": "Feature",
                  "properties": {
                    "feature_type": "route"
                  },
                  "geometry": {
                    "type": "LineString"
                  }
                }
              ]
            }
            """;
        String jsonCoordsNotArray = """
            {
              "type": "FeatureCollection",
              "features": [
                {
                  "type": "Feature",
                  "properties": {
                    "feature_type": "route"
                  },
                  "geometry": {
                    "type": "LineString",
                    "coordinates": "not-an-array"
                  }
                }
              ]
            }
            """;
        ObjectMapper mapper = new ObjectMapper();
        GeoJsonFeatureCollectionDTO payloadNoCoords = mapper.readValue(jsonNoCoords, GeoJsonFeatureCollectionDTO.class);
        GeoJsonFeatureCollectionDTO payloadNotArray = mapper.readValue(jsonCoordsNotArray, GeoJsonFeatureCollectionDTO.class);
        
        when(rutaRepository.save(any(Ruta.class))).thenAnswer(i -> i.getArguments()[0]);
        
        Ruta res1 = rutaService.agregarRutaDesdeGeoJson(payloadNoCoords);
        Ruta res2 = rutaService.agregarRutaDesdeGeoJson(payloadNotArray);
        
        assertTrue(res1.getCoordenadas().isEmpty());
        assertTrue(res2.getCoordenadas().isEmpty());
    }

    @Test
    void agregarRutaDesdeGeoJson_coordPairNotArrayOrSizeLessThanTwo() throws Exception {
        String json = """
            {
              "type": "FeatureCollection",
              "features": [
                {
                  "type": "Feature",
                  "properties": {
                    "feature_type": "route"
                  },
                  "geometry": {
                    "type": "LineString",
                    "coordinates": [
                      "not-an-array",
                      [1.0],
                      [1.0, 2.0]
                    ]
                  }
                }
              ]
            }
            """;
        ObjectMapper mapper = new ObjectMapper();
        GeoJsonFeatureCollectionDTO payload = mapper.readValue(json, GeoJsonFeatureCollectionDTO.class);
        when(rutaRepository.save(any(Ruta.class))).thenAnswer(i -> i.getArguments()[0]);
        
        Ruta res = rutaService.agregarRutaDesdeGeoJson(payload);
        assertEquals(1, res.getCoordenadas().size());
        assertEquals(1.0, res.getCoordenadas().get(0).getLongitud());
        assertEquals(2.0, res.getCoordenadas().get(0).getLatitud());
    }

    @Test
    void agregarRutaDesdeGeoJson_stopGeomMissingOrNoCoordinatesOrInvalidCoordinates() throws Exception {
        String json = """
            {
              "type": "FeatureCollection",
              "features": [
                {
                  "type": "Feature",
                  "properties": {
                    "feature_type": "route"
                  }
                },
                {
                  "type": "Feature",
                  "properties": {
                    "feature_type": "stop",
                    "stop_name": "Parada Sin Geom"
                  }
                },
                {
                  "type": "Feature",
                  "properties": {
                    "feature_type": "stop",
                    "stop_name": "Parada Geom No Coords"
                  },
                  "geometry": {
                    "type": "Point"
                  }
                },
                {
                  "type": "Feature",
                  "properties": {
                    "feature_type": "stop",
                    "stop_name": "Parada Coords Not Array"
                  },
                  "geometry": {
                    "type": "Point",
                    "coordinates": "not-array"
                  }
                },
                {
                  "type": "Feature",
                  "properties": {
                    "feature_type": "stop",
                    "stop_name": "Parada Coords Size One"
                  },
                  "geometry": {
                    "type": "Point",
                    "coordinates": [1.0]
                  }
                }
              ]
            }
            """;
        ObjectMapper mapper = new ObjectMapper();
        GeoJsonFeatureCollectionDTO payload = mapper.readValue(json, GeoJsonFeatureCollectionDTO.class);
        when(rutaRepository.save(any(Ruta.class))).thenAnswer(i -> i.getArguments()[0]);
        
        Ruta res = rutaService.agregarRutaDesdeGeoJson(payload);
        assertEquals(4, res.getParadas().size());
        assertNull(res.getParadas().get(0).getCoordenadasParada());
        assertNull(res.getParadas().get(1).getCoordenadasParada());
        assertNull(res.getParadas().get(2).getCoordenadasParada());
        assertNull(res.getParadas().get(3).getCoordenadasParada());
    }
}
