package buss.smartbussingapi.Viaje.CreatingOptimistPath;

import buss.smartbussingapi.Parada.Parada;
import buss.smartbussingapi.Parada.ParadaRepository;
import lombok.RequiredArgsConstructor;
import org.jgrapht.GraphPath;
import org.jgrapht.alg.interfaces.AStarAdmissibleHeuristic;
import org.jgrapht.alg.shortestpath.AStarShortestPath;
import org.jgrapht.graph.DefaultWeightedEdge;
import org.jgrapht.graph.SimpleWeightedGraph;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static buss.smartbussingapi.commons.Methods.haversine;

@Service
@RequiredArgsConstructor
public class AlgoService {

    private final GraphBuilderService graphBuilder;
    private final ParadaRepository paradaRepository;

    // Solo encuentra el camino óptimo entre dos paradas usando A*
    public List<Parada> findOptimalRoute(Parada origen, Parada destino) {
        SimpleWeightedGraph<Integer, DefaultWeightedEdge> graph = graphBuilder.getGraph();

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
}
