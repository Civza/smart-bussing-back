package buss.smartbussingapi.Viaje.CreatingOptimistPath;

import org.jgrapht.graph.DefaultDirectedWeightedGraph;
import org.jgrapht.graph.DefaultWeightedEdge;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AlgoServiceTest {

    @Mock
    private GraphBuilderService graphBuilderService;

    @InjectMocks
    private AlgoService algoService;

    private DefaultDirectedWeightedGraph<Integer, DefaultWeightedEdge> graph;
    private GraphBuilderService.RouteVertex vertex1;
    private GraphBuilderService.RouteVertex vertex2;

    @BeforeEach
    void setUp() {
        graph = new DefaultDirectedWeightedGraph<>(DefaultWeightedEdge.class);
        vertex1 = new GraphBuilderService.RouteVertex(1, 10, 0, 10.0, -84.0, "IDA");
        vertex2 = new GraphBuilderService.RouteVertex(2, 10, 1, 10.01, -84.01, "IDA");
    }

    @Test
    void testFindOptimalRoute_EmptyCandidates_ReturnsEmptyList() {
        when(graphBuilderService.getGraph()).thenReturn(graph);
        when(graphBuilderService.getNodeMap()).thenReturn(Collections.emptyMap());
        when(graphBuilderService.findNearestVertices(anyDouble(), anyDouble(), anyInt(), anyDouble()))
                .thenReturn(Collections.emptyList());

        List<GraphBuilderService.RouteVertex> result = algoService.findOptimalRoute(10.0, -84.0, 10.1, -84.1);

        assertTrue(result.isEmpty());
    }

    @Test
    void testFindOptimalRoute_NoPath_ReturnsEmptyList() {
        graph.addVertex(1);
        graph.addVertex(2);
        // No edge between 1 and 2

        when(graphBuilderService.getGraph()).thenReturn(graph);
        when(graphBuilderService.getNodeMap()).thenReturn(Map.of(1, vertex1, 2, vertex2));
        when(graphBuilderService.findNearestVertices(eq(10.0), eq(-84.0), anyInt(), anyDouble()))
                .thenReturn(List.of(vertex1));
        when(graphBuilderService.findNearestVertices(eq(10.5), eq(-84.5), anyInt(), anyDouble()))
                .thenReturn(List.of(vertex2));

        List<GraphBuilderService.RouteVertex> result = algoService.findOptimalRoute(10.0, -84.0, 10.5, -84.5);

        assertTrue(result.isEmpty());
    }

    @Test
    void testFindOptimalRoute_ValidPath_ReturnsPath() {
        graph.addVertex(1);
        graph.addVertex(2);
        DefaultWeightedEdge edge = graph.addEdge(1, 2);
        graph.setEdgeWeight(edge, 1.5); // some cost

        when(graphBuilderService.getGraph()).thenReturn(graph);
        when(graphBuilderService.getNodeMap()).thenReturn(Map.of(1, vertex1, 2, vertex2));
        
        when(graphBuilderService.findNearestVertices(eq(10.0), eq(-84.0), anyInt(), anyDouble()))
                .thenReturn(List.of(vertex1));
        // End coords far away so direct walk is not better
        when(graphBuilderService.findNearestVertices(eq(10.5), eq(-84.5), anyInt(), anyDouble()))
                .thenReturn(List.of(vertex2));

        List<GraphBuilderService.RouteVertex> result = algoService.findOptimalRoute(10.0, -84.0, 10.5, -84.5);

        assertEquals(2, result.size());
        assertEquals(1, result.get(0).nodeId());
        assertEquals(2, result.get(1).nodeId());
    }

    @Test
    void testFindOptimalRoute_DirectWalkBetter_ReturnsEmptyList() {
        graph.addVertex(1);
        graph.addVertex(2);
        DefaultWeightedEdge edge = graph.addEdge(1, 2);
        graph.setEdgeWeight(edge, 10.0); // high cost for bus

        when(graphBuilderService.getGraph()).thenReturn(graph);
        when(graphBuilderService.getNodeMap()).thenReturn(Map.of(1, vertex1, 2, vertex2));

        // Start and end are very close
        when(graphBuilderService.findNearestVertices(eq(10.0), eq(-84.0), anyInt(), anyDouble()))
                .thenReturn(List.of(vertex1));
        when(graphBuilderService.findNearestVertices(eq(10.001), eq(-84.001), anyInt(), anyDouble()))
                .thenReturn(List.of(vertex2));

        List<GraphBuilderService.RouteVertex> result = algoService.findOptimalRoute(10.0, -84.0, 10.001, -84.001);

        // Should return empty list because walk cost < bus cost and distance < 2.0km
        assertTrue(result.isEmpty());
    }
}
