package buss.smartbussingapi.Parada;

import buss.smartbussingapi.DTOs.GeoJsonStops.GeoJsonStopDTO;
import buss.smartbussingapi.DTOs.GeoJsonStops.GeoJsonStopGeometry;
import buss.smartbussingapi.DTOs.GeoJsonStops.GeoJsonStopProperties;
import buss.smartbussingapi.Ruta.Ruta;
import buss.smartbussingapi.Ruta.RutaRepository;
import buss.smartbussingapi.Viaje.CreatingOptimistPath.GraphBuilderService;
import buss.smartbussingapi.commons.exceptions.InvalidDataException;
import buss.smartbussingapi.commons.exceptions.NotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ParadaServiceTest {

    @Mock
    private ParadaRepository paradaRepository;

    @Mock
    private RutaRepository rutaRepository;

    @Mock
    private GraphBuilderService graphBuilderService;

    @InjectMocks
    private ParadaService paradaService;

    @Test
    void getParadasList_Success() {
        when(paradaRepository.findAll()).thenReturn(List.of(new Parada()));
        assertEquals(1, paradaService.getParadasList().size());
    }

    @Test
    void getParadaById_Success() {
        Parada p = new Parada();
        p.setIdParada(1);
        when(paradaRepository.findById(1)).thenReturn(Optional.of(p));

        assertEquals(1, paradaService.getParadaById(1).getIdParada());
    }

    @Test
    void getParadaById_NotFound() {
        when(paradaRepository.findById(1)).thenReturn(Optional.empty());
        assertThrows(NotFoundException.class, () -> paradaService.getParadaById(1));
    }

    @Test
    void addParada_InvalidFeatureType() {
        GeoJsonStopDTO dto = new GeoJsonStopDTO();
        dto.setProperties(new GeoJsonStopProperties());
        dto.getProperties().setFeatureType("route"); // should be "stop"

        assertThrows(InvalidDataException.class, () -> paradaService.addParada(dto));
    }

    @Test
    void addParada_InvalidGeometry() {
        GeoJsonStopDTO dto = new GeoJsonStopDTO();
        dto.setProperties(new GeoJsonStopProperties());
        dto.getProperties().setFeatureType("stop");
        dto.setGeometry(new GeoJsonStopGeometry("LineString", List.of())); // should be "Point"

        assertThrows(InvalidDataException.class, () -> paradaService.addParada(dto));
    }

    @Test
    void addParada_EmptyCoordinates() {
        GeoJsonStopDTO dto = new GeoJsonStopDTO();
        dto.setProperties(new GeoJsonStopProperties());
        dto.getProperties().setFeatureType("stop");
        dto.setGeometry(new GeoJsonStopGeometry("Point", Collections.emptyList()));

        assertThrows(InvalidDataException.class, () -> paradaService.addParada(dto));
    }

    @Test
    void addParada_InvalidLongitude() {
        GeoJsonStopDTO dto = new GeoJsonStopDTO();
        dto.setProperties(new GeoJsonStopProperties());
        dto.getProperties().setFeatureType("stop");
        dto.setGeometry(new GeoJsonStopGeometry("Point", List.of(190.0, 10.0)));

        assertThrows(InvalidDataException.class, () -> paradaService.addParada(dto));
    }

    @Test
    void addParada_InvalidLatitude() {
        GeoJsonStopDTO dto = new GeoJsonStopDTO();
        dto.setProperties(new GeoJsonStopProperties());
        dto.getProperties().setFeatureType("stop");
        dto.setGeometry(new GeoJsonStopGeometry("Point", List.of(10.0, 95.0)));

        assertThrows(InvalidDataException.class, () -> paradaService.addParada(dto));
    }

    @Test
    void addParada_RouteNotFound() {
        GeoJsonStopDTO dto = new GeoJsonStopDTO();
        GeoJsonStopProperties props = new GeoJsonStopProperties();
        props.setFeatureType("stop");
        props.setRoutesNames(List.of("Ruta 99"));
        dto.setProperties(props);
        dto.setGeometry(new GeoJsonStopGeometry("Point", List.of(-84.0, 10.0)));

        when(rutaRepository.findRutaByNombreRuta("Ruta 99")).thenReturn(null);

        assertThrows(NotFoundException.class, () -> paradaService.addParada(dto));
    }

    @Test
    void addParada_Success() {
        GeoJsonStopDTO dto = new GeoJsonStopDTO();
        GeoJsonStopProperties props = new GeoJsonStopProperties();
        props.setFeatureType("stop");
        props.setStopName("Parada 1");
        props.setRoutesNames(List.of("Ruta 1"));
        dto.setProperties(props);
        dto.setGeometry(new GeoJsonStopGeometry("Point", List.of(-84.0, 10.0)));

        Ruta mockRuta = new Ruta();
        mockRuta.setParadas(new ArrayList<>());
        when(rutaRepository.findRutaByNombreRuta("Ruta 1")).thenReturn(mockRuta);
        
        when(paradaRepository.save(any(Parada.class))).thenAnswer(i -> i.getArgument(0));

        Parada result = paradaService.addParada(dto);

        assertNotNull(result);
        assertEquals("Parada 1", result.getNombreParada());
        assertEquals(-84.0, result.getCoordenadasParada().getLongitud());
        assertEquals(10.0, result.getCoordenadasParada().getLatitud());
        assertEquals(1, mockRuta.getParadas().size()); // Added to route
        verify(graphBuilderService, times(1)).rebuildGraph();
    }

    @Test
    void addParada_NullRoutesNames() {
        GeoJsonStopDTO dto = new GeoJsonStopDTO();
        GeoJsonStopProperties props = new GeoJsonStopProperties();
        props.setFeatureType("stop");
        props.setStopName("Parada Sin Rutas");
        props.setRoutesNames(null);
        dto.setProperties(props);
        dto.setGeometry(new GeoJsonStopGeometry("Point", List.of(-84.0, 10.0)));

        when(paradaRepository.save(any(Parada.class))).thenAnswer(i -> i.getArgument(0));

        Parada result = paradaService.addParada(dto);

        assertNotNull(result);
        assertEquals("Parada Sin Rutas", result.getNombreParada());
        verify(rutaRepository, never()).findRutaByNombreRuta(anyString());
        verify(graphBuilderService, times(1)).rebuildGraph();
    }
}
