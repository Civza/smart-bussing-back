package buss.smartbussingapi.Viaje;

import buss.smartbussingapi.DTOs.DirectionsResponse;
import buss.smartbussingapi.DTOs.ItineraryDTOs.ItineraryResponseDTO;
import buss.smartbussingapi.DTOs.ItineraryDTOs.SegmentoResponseDTO;
import buss.smartbussingapi.Viaje.CreatingOptimistPath.AlgoService;
import buss.smartbussingapi.Viaje.CreatingOptimistPath.GraphBuilderService;
import buss.smartbussingapi.Viaje.CreatingOptimistPath.MapboxService;
import buss.smartbussingapi.Viaje.CreatingOptimistPath.PathSegmenterService;
import buss.smartbussingapi.commons.exceptions.NotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ViajesService {

    private final ViajeRepository viajeRepository;
    private final MapboxService mapboxService;
    private final AlgoService algoService;
    private final PathSegmenterService pathSegmenter;

    public Viaje getViajebyId(int id_viaje) {
        return viajeRepository.findById(id_viaje)
                .orElseThrow(() -> new NotFoundException("Viaje with ID " + id_viaje + " not found"));
    }

    public List<Viaje> getAllViajes() {
        return viajeRepository.findAll();
    }

    public ItineraryResponseDTO getDraftRoute(double userLat, double userLon, double destLat, double destLon) {
        // 1. Calculate the core path using polyline-vertex graph
        List<GraphBuilderService.RouteVertex> corePath = algoService.findOptimalRoute(userLat, userLon, destLat, destLon);
        
        if (corePath.isEmpty()) {
            throw new NotFoundException("No se encontró una ruta de autobús disponible para este trayecto.");
        }

        // 2. Segment the path into BUS and TRANSFER segments
        List<SegmentoResponseDTO> busSegments = pathSegmenter.segmentPath(corePath);

        // 3. Assemble final itinerary with initial and final walking legs
        List<SegmentoResponseDTO> finalSegments = new ArrayList<>();
        double totalSeconds = 0;
        double totalMeters = 0;

        // Initial walk: User -> First RouteVertex
        GraphBuilderService.RouteVertex first = corePath.get(0);
        DirectionsResponse walkToStart = mapboxService.getWalkingDirections(userLat, userLon, first.lat(), first.lon());
        finalSegments.add(SegmentoResponseDTO.builder()
                .tipo("WALKING")
                .descripcion("Caminar hasta el punto de abordaje")
                .directions(walkToStart)
                .build());
        totalSeconds += walkToStart.getTimeSeconds();
        totalMeters += walkToStart.getDistanceMeters();

        // Bus & Transfer segments
        for (SegmentoResponseDTO segment : busSegments) {
            finalSegments.add(segment);
            totalSeconds += segment.getDirections().getTimeSeconds();
            totalMeters += segment.getDirections().getDistanceMeters();
        }

        // Final walk: Last RouteVertex -> Destination
        GraphBuilderService.RouteVertex last = corePath.get(corePath.size() - 1);
        DirectionsResponse walkToDest = mapboxService.getWalkingDirections(last.lat(), last.lon(), destLat, destLon);
        finalSegments.add(SegmentoResponseDTO.builder()
                .tipo("WALKING")
                .descripcion("Caminar hasta tu destino final")
                .directions(walkToDest)
                .build());
        totalSeconds += walkToDest.getTimeSeconds();
        totalMeters += walkToDest.getDistanceMeters();

        return ItineraryResponseDTO.builder()
                .segmentos(finalSegments)
                .duracionTotalSegundos(totalSeconds)
                .distanciaTotalMetros(totalMeters)
                .build();
    }
}
