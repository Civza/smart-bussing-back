package buss.smartbussingapi.Viaje.CreatingOptimistPath;

import lombok.RequiredArgsConstructor;
import org.jgrapht.GraphPath;
import org.jgrapht.alg.interfaces.AStarAdmissibleHeuristic;
import org.jgrapht.alg.shortestpath.AStarShortestPath;
import org.jgrapht.graph.DefaultWeightedEdge;
import org.jgrapht.graph.DefaultDirectedWeightedGraph;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import static buss.smartbussingapi.commons.Methods.haversine;

@Service
@RequiredArgsConstructor
public class AlgoService {

    private final GraphBuilderService graphBuilder;

    /**
     * Finds the optimal path between two coordinates using A* on the polyline-vertex graph.
     * Returns a list of RouteVertex metadata representing the path.
     */
    public List<GraphBuilderService.RouteVertex> findOptimalRoute(
            double startLat, double startLon, double endLat, double endLon) {

        DefaultDirectedWeightedGraph<Integer, DefaultWeightedEdge> graph = graphBuilder.getGraph();
        Map<Integer, GraphBuilderService.RouteVertex> nodeMap = graphBuilder.getNodeMap();

        Integer startNodeId = graphBuilder.findNearestVertex(startLat, startLon);
        Integer endNodeId = graphBuilder.findNearestVertex(endLat, endLon);

        if (startNodeId == null || endNodeId == null) return Collections.emptyList();

        GraphBuilderService.RouteVertex target = nodeMap.get(endNodeId);

        AStarAdmissibleHeuristic<Integer> heuristic = (nodeId, targetId) -> {
            GraphBuilderService.RouteVertex current = nodeMap.get(nodeId);
            return haversine(current.lat(), current.lon(), target.lat(), target.lon());
        };

        AStarShortestPath<Integer, DefaultWeightedEdge> aStar =
                new AStarShortestPath<>(graph, heuristic);

        GraphPath<Integer, DefaultWeightedEdge> path = aStar.getPath(startNodeId, endNodeId);

        if (path == null) return Collections.emptyList();

        return path.getVertexList().stream()
                .map(nodeMap::get)
                .toList();
    }
}
