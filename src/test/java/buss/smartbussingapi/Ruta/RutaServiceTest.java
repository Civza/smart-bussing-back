package buss.smartbussingapi.Ruta;

import buss.smartbussingapi.DTOs.RutaRequestDTO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
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

}
