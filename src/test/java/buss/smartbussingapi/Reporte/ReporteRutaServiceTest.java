package buss.smartbussingapi.Reporte;

import buss.smartbussingapi.DTOs.ReporteRutaDTO;
import buss.smartbussingapi.Ruta.Ruta;
import buss.smartbussingapi.Ruta.RutaRepository;
import buss.smartbussingapi.Usuario.Usuario;
import buss.smartbussingapi.Usuario.UsuarioRepository;
import buss.smartbussingapi.commons.exceptions.NotFoundException;
import buss.smartbussingapi.commons.exceptions.ResourceBlockedException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReporteRutaServiceTest {

    @Mock
    private ReporteRutaRepository reporteRutaRepository;

    @Mock
    private RutaRepository rutaRepository;

    @Mock
    private UsuarioRepository usuarioRepository;

    @InjectMocks
    private ReporteRutaService reporteRutaService;

    private ReporteRuta reporteMock;

    @BeforeEach
    void setUp() {
        reporteMock = new ReporteRuta();
        reporteMock.setIdReporteRuta(1);
    }

    @Test
    void getReportesRuta_Success() {
        when(reporteRutaRepository.findAll()).thenReturn(List.of(reporteMock));

        List<ReporteRuta> result = reporteRutaService.getReportesRuta();

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(1, result.get(0).getIdReporteRuta());
        verify(reporteRutaRepository, times(1)).findAll();
    }

    @Test
    void getReportesRutaByRouteName_Success() {
        when(reporteRutaRepository.getReporteRutasByRouteName("Ruta 1"))
                .thenReturn(List.of(reporteMock));

        List<ReporteRuta> result = reporteRutaService.getReportesRutaByRouteName("Ruta 1");

        assertNotNull(result);
        assertEquals(1, result.size());
        verify(reporteRutaRepository, times(1)).getReporteRutasByRouteName("Ruta 1");
    }

    @Test
    void getReportesRutaByRouteName_NotFound() {
        when(reporteRutaRepository.getReporteRutasByRouteName("Ruta Inexistente"))
                .thenReturn(Collections.emptyList());

        assertThrows(NotFoundException.class, () ->
                reporteRutaService.getReportesRutaByRouteName("Ruta Inexistente")
        );
        verify(reporteRutaRepository, times(1)).getReporteRutasByRouteName("Ruta Inexistente");
    }

    @Test
    void getReporteRutaById_Success() {
        when(reporteRutaRepository.findById(1)).thenReturn(Optional.of(reporteMock));

        ReporteRuta result = reporteRutaService.getReporteRutaById(1);

        assertNotNull(result);
        assertEquals(1, result.getIdReporteRuta());
        verify(reporteRutaRepository, times(1)).findById(1);
    }

    @Test
    void getReporteRutaById_NotFound() {
        when(reporteRutaRepository.findById(99)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () ->
                reporteRutaService.getReporteRutaById(99)
        );
        verify(reporteRutaRepository, times(1)).findById(99);
    }

    @Test
    void createNewReporteRuta_Success() {
        ReporteRutaDTO dto = new ReporteRutaDTO();
        dto.setDescripcion("Buen viaje");
        dto.setUrlPhoto(new String[]{"url1"});

        Ruta r = new Ruta();
        r.setIdRuta(1);

        Usuario u = new Usuario();
        u.setEmail("test@test.com");

        when(rutaRepository.findById(1)).thenReturn(Optional.of(r));
        when(usuarioRepository.findByEmail("test@test.com")).thenReturn(Optional.of(u));
        when(reporteRutaRepository.save(any(ReporteRuta.class))).thenAnswer(i -> i.getArgument(0));

        ReporteRuta result = reporteRutaService.createNewReporteRuta(dto, 1, "test@test.com");
        
        assertEquals("Buen viaje", result.getDescripcion());
        assertEquals("url1", result.getUrlPhotos()[0]);
        assertEquals(r, result.getRuta());
        assertEquals(u, result.getUsuario());
        assertEquals(0, result.getLikeRoute());
    }

    @Test
    void createNewReporteRuta_RouteNotFound() {
        ReporteRutaDTO dto = new ReporteRutaDTO();
        when(rutaRepository.findById(1)).thenReturn(Optional.empty());
        
        assertThrows(NotFoundException.class, () -> reporteRutaService.createNewReporteRuta(dto, 1, "test@test.com"));
    }

    @Test
    void createNewReporteRuta_RouteBlocked() {
        ReporteRutaDTO dto = new ReporteRutaDTO();
        Ruta r = new Ruta();
        r.setIdRuta(1);
        r.setActive(false);
        when(rutaRepository.findById(1)).thenReturn(Optional.of(r));

        assertThrows(ResourceBlockedException.class, () -> reporteRutaService.createNewReporteRuta(dto, 1, "test@test.com"));
    }

    @Test
    void createNewReporteRuta_UserNotFound() {
        ReporteRutaDTO dto = new ReporteRutaDTO();
        Ruta r = new Ruta();
        
        when(rutaRepository.findById(1)).thenReturn(Optional.of(r));
        when(usuarioRepository.findByEmail("test@test.com")).thenReturn(Optional.empty());
        
        assertThrows(NotFoundException.class, () -> reporteRutaService.createNewReporteRuta(dto, 1, "test@test.com"));
    }
}
