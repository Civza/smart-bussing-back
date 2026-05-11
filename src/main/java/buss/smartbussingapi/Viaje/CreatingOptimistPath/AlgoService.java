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

    private static final double BOARDING_PENALTY_KM = 1.0;
    private static final int MAX_CANDIDATES = 5;
    private static final double MAX_SEARCH_DIST_KM = 2.0;

    /**
     * Finds the optimal path between two coordinates using A* on the polyline-vertex graph.
     * Evaluates multiple candidate entry/exit points to find the best overall trip.
     */
    public List<GraphBuilderService.RouteVertex> findOptimalRoute(
            double startLat, double startLon, double endLat, double endLon) {

        DefaultDirectedWeightedGraph<Integer, DefaultWeightedEdge> graph = graphBuilder.getGraph();
        Map<Integer, GraphBuilderService.RouteVertex> nodeMap = graphBuilder.getNodeMap();

        List<GraphBuilderService.RouteVertex> startCandidates = 
                graphBuilder.findNearestVertices(startLat, startLon, MAX_CANDIDATES, MAX_SEARCH_DIST_KM);
        List<GraphBuilderService.RouteVertex> endCandidates = 
                graphBuilder.findNearestVertices(endLat, endLon, MAX_CANDIDATES, MAX_SEARCH_DIST_KM);

        if (startCandidates.isEmpty() || endCandidates.isEmpty()) return Collections.emptyList();

        List<GraphBuilderService.RouteVertex> bestPath = Collections.emptyList();
        double minTotalCost = Double.MAX_VALUE;

        for (GraphBuilderService.RouteVertex start : startCandidates) {
            for (GraphBuilderService.RouteVertex end : endCandidates) {
                
                AStarAdmissibleHeuristic<Integer> heuristic = (nodeId, targetId) -> {
                    GraphBuilderService.RouteVertex current = nodeMap.get(nodeId);
                    return haversine(current.lat(), current.lon(), end.lat(), end.lon());
                };

                AStarShortestPath<Integer, DefaultWeightedEdge> aStar =
                        new AStarShortestPath<>(graph, heuristic);

                GraphPath<Integer, DefaultWeightedEdge> path = aStar.getPath(start.nodeId(), end.nodeId());

                if (path != null) {
                    double walkToStart = haversine(startLat, startLon, start.lat(), start.lon());
                    double walkFromEnd = haversine(endLat, endLon, end.lat(), end.lon());
                    
                    // Total cost = walk + boarding + graph_weight
                    double totalCost = (walkToStart * GraphBuilderService.WALK_PENALTY_FACTOR) 
                                     + path.getWeight() 
                                     + (walkFromEnd * GraphBuilderService.WALK_PENALTY_FACTOR) 
                                     + BOARDING_PENALTY_KM;

                    if (totalCost < minTotalCost) {
                        minTotalCost = totalCost;
                        bestPath = path.getVertexList().stream()
                                .map(nodeMap::get)
                                .toList();
                    }
                }
            }
        }

        // Final check: Is direct walking better than the best bus option?
        double directWalkCost = haversine(startLat, startLon, endLat, endLon) * GraphBuilderService.WALK_PENALTY_FACTOR;
        if (directWalkCost < minTotalCost) {
            return Collections.emptyList(); // AlgoService returns empty to signal "just walk"
        }

        return bestPath;
    }
}
