package buss.smartbussingapi.Viaje;

import buss.smartbussingapi.DTOs.DirectionsResponse;
import buss.smartbussingapi.DTOs.ItineraryDTOs.ItineraryResponseDTO;
import buss.smartbussingapi.DTOs.ItineraryDTOs.SegmentoResponseDTO;
import buss.smartbussingapi.Viaje.CreatingOptimistPath.AlgoService;
import buss.smartbussingapi.Viaje.CreatingOptimistPath.GraphBuilderService;
import buss.smartbussingapi.Viaje.CreatingOptimistPath.MapboxService;
import buss.smartbussingapi.Viaje.CreatingOptimistPath.PathSegmenterService;
import buss.smartbussingapi.commons.exceptions.NotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ViajesServiceTest {

    @Mock
    private ViajeRepository viajeRepository;
    @Mock
    private MapboxService mapboxService;
    @Mock
    private AlgoService algoService;
    @Mock
    private PathSegmenterService pathSegmenter;

    @InjectMocks
    private ViajesService viajesService;

    @Test
    void testGetViajebyId_Success() {
        Viaje v = new Viaje();
        v.setId_viaje(1);
        when(viajeRepository.findById(1)).thenReturn(Optional.of(v));

        Viaje result = viajesService.getViajebyId(1);
        assertEquals(1, result.getId_viaje());
    }

    @Test
    void testGetViajebyId_NotFound() {
        when(viajeRepository.findById(1)).thenReturn(Optional.empty());
        assertThrows(NotFoundException.class, () -> viajesService.getViajebyId(1));
    }

    @Test
    void testGetAllViajes() {
        when(viajeRepository.findAll()).thenReturn(List.of(new Viaje()));
        assertEquals(1, viajesService.getAllViajes().size());
    }

    @Test
    void testGetDraftRoute_DirectWalk() {
        when(algoService.findOptimalRoute(anyDouble(), anyDouble(), anyDouble(), anyDouble()))
                .thenReturn(Collections.emptyList());

        DirectionsResponse walk = DirectionsResponse.builder()
                .distanceMeters(500.0)
                .timeSeconds(300.0)
                .build();
        
        when(mapboxService.getWalkingDirections(10.0, -84.0, 10.01, -84.01)).thenReturn(walk);

        ItineraryResponseDTO response = viajesService.getDraftRoute(10.0, -84.0, 10.01, -84.01);

        assertEquals(1, response.getSegmentos().size());
        assertEquals("WALKING", response.getSegmentos().get(0).getTipo());
        assertEquals(500.0, response.getDistanciaTotalMetros());
        assertEquals(300.0, response.getDuracionTotalSegundos());
    }

    @Test
    void testGetDraftRoute_WithBusPath() {
        GraphBuilderService.RouteVertex v1 = new GraphBuilderService.RouteVertex(1, 1, 0, 10.001, -84.001, "IDA");
        GraphBuilderService.RouteVertex v2 = new GraphBuilderService.RouteVertex(2, 1, 1, 10.009, -84.009, "IDA");
        
        List<GraphBuilderService.RouteVertex> corePath = List.of(v1, v2);

        when(algoService.findOptimalRoute(10.0, -84.0, 10.01, -84.01)).thenReturn(corePath);

        SegmentoResponseDTO busSeg = SegmentoResponseDTO.builder()
                .tipo("BUS")
                .directions(DirectionsResponse.builder().distanceMeters(1000.0).timeSeconds(200.0).build())
                .build();

        when(pathSegmenter.segmentPath(corePath)).thenReturn(List.of(busSeg));

        DirectionsResponse startWalk = DirectionsResponse.builder().distanceMeters(100.0).timeSeconds(120.0).build();
        DirectionsResponse endWalk = DirectionsResponse.builder().distanceMeters(100.0).timeSeconds(120.0).build();

        when(mapboxService.getWalkingDirections(10.0, -84.0, v1.lat(), v1.lon())).thenReturn(startWalk);
        when(mapboxService.getWalkingDirections(v2.lat(), v2.lon(), 10.01, -84.01)).thenReturn(endWalk);

        ItineraryResponseDTO response = viajesService.getDraftRoute(10.0, -84.0, 10.01, -84.01);

        assertEquals(3, response.getSegmentos().size()); // Walk, Bus, Walk
        assertEquals(1200.0, response.getDistanciaTotalMetros()); // 100 + 1000 + 100
        assertEquals(440.0, response.getDuracionTotalSegundos()); // 120 + 200 + 120
    }
}
