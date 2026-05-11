package buss.smartbussingapi.Viaje.CreatingOptimistPath;

import buss.smartbussingapi.DTOs.DirectionsResponse;
import buss.smartbussingapi.DTOs.GeoJsonRoute.GeoJsonRouteGeometry;
import buss.smartbussingapi.DTOs.ItineraryDTOs.SegmentoResponseDTO;
import buss.smartbussingapi.Ruta.Ruta;
import buss.smartbussingapi.Ruta.RutaRepository;
import buss.smartbussingapi.commons.exceptions.NotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static buss.smartbussingapi.commons.Methods.haversine;

@Service
@RequiredArgsConstructor
public class PathSegmenterService {

    private final RutaRepository rutaRepository;

    public List<SegmentoResponseDTO> segmentPath(List<GraphBuilderService.RouteVertex> path) {
        if (path == null || path.isEmpty()) return Collections.emptyList();

        List<SegmentoResponseDTO> segments = new ArrayList<>();
        List<GraphBuilderService.RouteVertex> currentGroup = new ArrayList<>();
        int currentRutaId = path.get(0).rutaId();

        for (int i = 0; i < path.size(); i++) {
            GraphBuilderService.RouteVertex v = path.get(i);

            if (v.rutaId() == currentRutaId) {
                // Potential gap check (intra-route transfer)
                if (!currentGroup.isEmpty()) {
                    GraphBuilderService.RouteVertex last = currentGroup.get(currentGroup.size() - 1);
                    if (Math.abs(v.vertexIndex() - last.vertexIndex()) > 1) {
                        segments.add(createBusSegment(currentGroup));
                        segments.add(createWalkSegment(last, v));
                        currentGroup = new ArrayList<>();
                    }
                }
                currentGroup.add(v);
            } else {
                // Route changed! Flush current group and add walking transfer
                if (!currentGroup.isEmpty()) {
                    segments.add(createBusSegment(currentGroup));
                    segments.add(createWalkSegment(currentGroup.get(currentGroup.size() - 1), v));
                }
                currentGroup = new ArrayList<>();
                currentGroup.add(v);
                currentRutaId = v.rutaId();
            }
        }

        if (!currentGroup.isEmpty()) {
            segments.add(createBusSegment(currentGroup));
        }

        return segments;
    }

    private SegmentoResponseDTO createBusSegment(List<GraphBuilderService.RouteVertex> vertices) {
        if (vertices.isEmpty()) return null;
        int rutaId = vertices.get(0).rutaId();
        Ruta ruta = rutaRepository.findById(rutaId).orElseThrow(() -> new NotFoundException("Ruta " + rutaId));

        List<List<Double>> coords = vertices.stream().map(v -> List.of(v.lon(), v.lat())).collect(Collectors.toList());
        double distKm = calculateDistance(vertices);

        DirectionsResponse directions = DirectionsResponse.builder()
                .distanceMeters(distKm * 1000)
                .timeSeconds(distKm * 150) // Estimate: 24 km/h
                .geoJson(new GeoJsonRouteGeometry("LineString", coords))
                .build();

        return SegmentoResponseDTO.builder()
                .tipo("BUS")
                .descripcion("Subir al autobús: " + ruta.getNombre_ruta())
                .directions(directions)
                .build();
    }

    private SegmentoResponseDTO createWalkSegment(GraphBuilderService.RouteVertex from, GraphBuilderService.RouteVertex to) {
        double distKm = haversine(from.lat(), from.lon(), to.lat(), to.lon());
        List<List<Double>> coords = List.of(List.of(from.lon(), from.lat()), List.of(to.lon(), to.lat()));

        DirectionsResponse directions = DirectionsResponse.builder()
                .distanceMeters(distKm * 1000)
                .timeSeconds(distKm * 1200) // Estimate: 3 km/h
                .geoJson(new GeoJsonRouteGeometry("LineString", coords))
                .build();

        return SegmentoResponseDTO.builder()
                .tipo("WALKING")
                .descripcion("Transbordo: caminar hacia la siguiente ruta")
                .directions(directions)
                .build();
    }

    private double calculateDistance(List<GraphBuilderService.RouteVertex> vertices) {
        double total = 0;
        for (int i = 0; i < vertices.size() - 1; i++) {
            total += haversine(vertices.get(i).lat(), vertices.get(i).lon(), vertices.get(i+1).lat(), vertices.get(i+1).lon());
        }
        return total;
    }
}
