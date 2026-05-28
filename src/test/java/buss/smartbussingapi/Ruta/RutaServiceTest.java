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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
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
        assertEquals("Ruta de prueba", result.getNombre_ruta());
        assertEquals("RP", result.getNombre_corto_ruta());
        assertEquals("#FF0000", result.getColor_ruta());
        assertEquals("#FFFFFF", result.getColor_texto_ruta());
        assertEquals(RutaType.MICROBUS, result.getTipo_ruta());
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
        r.setId_ruta(1);
        r.setNombre_ruta("Test Ruta");
        when(rutaRepository.findById(1)).thenReturn(java.util.Optional.of(r));
        Ruta result = rutaService.getRutaById(1);
        assertNotNull(result);
        assertEquals("Test Ruta", result.getNombre_ruta());
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
        r.setId_ruta(1);
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
        r.setId_ruta(1);
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
        assertEquals(RutaType.URBANA, res.getTipo_ruta());
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
        
        assertEquals("", res.getNombre_ruta());
        assertEquals("", res.getNombre_corto_ruta());
        assertEquals("#000000", res.getColor_ruta());
        assertEquals("#FFFFFF", res.getColor_texto_ruta());
        assertEquals(RutaType.URBANA, res.getTipo_ruta());
        assertTrue(res.isBidirectional());
        assertEquals("AMBOS", res.getCoordenadas().get(0).getSentido());
    }
}
