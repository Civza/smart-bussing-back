package buss.smartbussingapi.Viaje.CreatingOptimistPath;

import buss.smartbussingapi.DTOs.ItineraryDTOs.SegmentoResponseDTO;
import buss.smartbussingapi.Ruta.Ruta;
import buss.smartbussingapi.Ruta.RutaRepository;
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
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PathSegmenterServiceTest {

    @Mock
    private RutaRepository rutaRepository;

    @InjectMocks
    private PathSegmenterService pathSegmenterService;

    private Ruta ruta1;
    private Ruta ruta2;

    @BeforeEach
    void setUp() {
        ruta1 = new Ruta();
        ruta1.setId_ruta(1);
        ruta1.setNombre_ruta("Ruta 1");

        ruta2 = new Ruta();
        ruta2.setId_ruta(2);
        ruta2.setNombre_ruta("Ruta 2");
    }

    @Test
    void testSegmentPath_NullOrEmpty_ReturnsEmpty() {
        assertTrue(pathSegmenterService.segmentPath(null).isEmpty());
        assertTrue(pathSegmenterService.segmentPath(Collections.emptyList()).isEmpty());
    }

    @Test
    void testSegmentPath_ContinuousPath() {
        when(rutaRepository.findById(1)).thenReturn(Optional.of(ruta1));

        List<GraphBuilderService.RouteVertex> path = List.of(
                new GraphBuilderService.RouteVertex(1, 1, 0, 10.0, -84.0, "IDA"),
                new GraphBuilderService.RouteVertex(2, 1, 1, 10.1, -84.1, "IDA")
        );

        List<SegmentoResponseDTO> segments = pathSegmenterService.segmentPath(path);

        assertEquals(1, segments.size());
        assertEquals("BUS", segments.get(0).getTipo());
        assertEquals("Subir al autobús: Ruta 1", segments.get(0).getDescripcion());
        assertNotNull(segments.get(0).getDirections());
    }

    @Test
    void testSegmentPath_GapInSameRoute() {
        when(rutaRepository.findById(1)).thenReturn(Optional.of(ruta1));

        List<GraphBuilderService.RouteVertex> path = List.of(
                new GraphBuilderService.RouteVertex(1, 1, 0, 10.0, -84.0, "IDA"),
                // Gap in index (jump > 1) means we took a shortcut / intra-route transfer
                new GraphBuilderService.RouteVertex(2, 1, 5, 10.5, -84.5, "IDA")
        );

        List<SegmentoResponseDTO> segments = pathSegmenterService.segmentPath(path);

        // Expected: BUS (for node 0), WALK (0 to 5), BUS (for node 5)
        assertEquals(3, segments.size());
        assertEquals("BUS", segments.get(0).getTipo());
        assertEquals("WALKING", segments.get(1).getTipo());
        assertEquals("BUS", segments.get(2).getTipo());
    }

    @Test
    void testSegmentPath_RouteChange() {
        when(rutaRepository.findById(1)).thenReturn(Optional.of(ruta1));
        when(rutaRepository.findById(2)).thenReturn(Optional.of(ruta2));

        List<GraphBuilderService.RouteVertex> path = List.of(
                new GraphBuilderService.RouteVertex(1, 1, 0, 10.0, -84.0, "IDA"),
                new GraphBuilderService.RouteVertex(2, 2, 0, 10.1, -84.1, "IDA")
        );

        List<SegmentoResponseDTO> segments = pathSegmenterService.segmentPath(path);

        // Expected: BUS (ruta 1), WALK (transfer), BUS (ruta 2)
        assertEquals(3, segments.size());
        assertEquals("BUS", segments.get(0).getTipo());
        assertEquals("Subir al autobús: Ruta 1", segments.get(0).getDescripcion());
        
        assertEquals("WALKING", segments.get(1).getTipo());
        
        assertEquals("BUS", segments.get(2).getTipo());
        assertEquals("Subir al autobús: Ruta 2", segments.get(2).getDescripcion());
    }

    @Test
    void testSegmentPath_RutaNotFound() {
        when(rutaRepository.findById(anyInt())).thenReturn(Optional.empty());

        List<GraphBuilderService.RouteVertex> path = List.of(
                new GraphBuilderService.RouteVertex(1, 1, 0, 10.0, -84.0, "IDA")
        );

        assertThrows(NotFoundException.class, () -> pathSegmenterService.segmentPath(path));
    }
}
