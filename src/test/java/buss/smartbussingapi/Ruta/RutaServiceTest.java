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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class RutaServiceTest {

    @Mock
    RutaRepository rutaRepository;

    @InjectMocks
    RutaService rutaService;

    private Ruta buildMockSavedRuta(int id_ruta, String nombre_ruta, String nombre_corto_ruta, String color_ruta, String color_texto_ruta, String tipo_ruta, String horario_ruta, boolean active) {
        var ruta = new Ruta(id_ruta, nombre_ruta, nombre_corto_ruta, color_ruta, color_texto_ruta, tipo_ruta, horario_ruta, active, null, null, null);
        return ruta;
    };
    
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
                    "route_type": "bus"
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
        assertEquals("bus", result.getTipo_ruta());
        assertTrue(result.isActive());
        
        assertNotNull(result.getCoordenadas());
        assertEquals(2, result.getCoordenadas().size());
        assertEquals(-116.59, result.getCoordenadas().get(0).getLongitud());
        assertEquals(31.86, result.getCoordenadas().get(0).getLatitud());
        assertEquals(-116.60, result.getCoordenadas().get(1).getLongitud());
        assertEquals(31.87, result.getCoordenadas().get(1).getLatitud());
        
        assertNotNull(result.getParadas());
        assertEquals(1, result.getParadas().size());
        assertEquals("Parada 1", result.getParadas().get(0).getNombre_parada());
        assertEquals("Primera parada", result.getParadas().get(0).getDescripcion_parada());
        assertNotNull(result.getParadas().get(0).getCoordenadas_parada());
        assertEquals(-116.595, result.getParadas().get(0).getCoordenadas_parada().getLongitud());
        assertEquals(31.865, result.getParadas().get(0).getCoordenadas_parada().getLatitud());
        
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
}
