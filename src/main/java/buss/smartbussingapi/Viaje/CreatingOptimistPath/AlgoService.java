package buss.smartbussingapi.Viaje.CreatingOptimistPath;

import buss.smartbussingapi.DTOs.GeoJsonRoute.GeoJsonRouteGeometry;
import buss.smartbussingapi.Parada.Parada;
import buss.smartbussingapi.Parada.ParadaRepository;
import lombok.RequiredArgsConstructor;
import org.jgrapht.GraphPath;
import org.jgrapht.alg.interfaces.AStarAdmissibleHeuristic;
import org.jgrapht.alg.shortestpath.AStarShortestPath;
import org.jgrapht.graph.DefaultWeightedEdge;
import org.jgrapht.graph.SimpleWeightedGraph;
import org.springframework.stereotype.Service;

import java.awt.*;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AlgoService {

    private final GraphBuilderService graphBuilder;
    private final ParadaRepository paradaRepository;

    // RouteService.java — solo encuentra el camino
    public List<Parada> findOptimalRoute(Parada origen, Parada destino) {
        SimpleWeightedGraph<Integer, DefaultWeightedEdge> graph = graphBuilder.buildGraph();

        Map<Integer, Parada> paradaMap = paradaRepository.findAll()
                .stream().collect(Collectors.toMap(Parada::getId_parada, p -> p));

        AStarAdmissibleHeuristic<Integer> heuristic = (nodeId, targetId) -> {
            Parada current = paradaMap.get(nodeId);
            return haversine(
                    current.getCoordenadas_parada().getLatitud(),
                    current.getCoordenadas_parada().getLongitud(),
                    destino.getCoordenadas_parada().getLatitud(),
                    destino.getCoordenadas_parada().getLongitud()
            );
        };

        AStarShortestPath<Integer, DefaultWeightedEdge> aStar =
                new AStarShortestPath<>(graph, heuristic);

        GraphPath<Integer, DefaultWeightedEdge> path =
                aStar.getPath(origen.getId_parada(), destino.getId_parada());

        if (path == null) return Collections.emptyList();

        return path.getVertexList().stream()
                .map(paradaMap::get)
                .toList();
    }

    // ── Haversine ────────────────────────────────────────────────────────────
    private double haversine(double lat1, double lon1, double lat2, double lon2) {
        double R = 6371;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat/2) * Math.sin(dLat/2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon/2) * Math.sin(dLon/2);
        return R * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1-a));
    }
}
