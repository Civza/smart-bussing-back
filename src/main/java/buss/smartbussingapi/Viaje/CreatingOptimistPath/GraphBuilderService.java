package buss.smartbussingapi.Viaje.CreatingOptimistPath;

import buss.smartbussingapi.Coordenadas.Coordenadas;
import buss.smartbussingapi.Ruta.Ruta;
import buss.smartbussingapi.Ruta.RutaRepository;
import jakarta.annotation.PostConstruct;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jgrapht.graph.DefaultWeightedEdge;
import org.jgrapht.graph.DefaultDirectedWeightedGraph;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

import static buss.smartbussingapi.commons.Methods.haversine;

@Service
@RequiredArgsConstructor
@Slf4j
public class GraphBuilderService {

    private final RutaRepository rutaRepository;

    // ── Constants ────────────────────────────────────────────────────────────
    /** Max walk distance (km) between two route vertices to create a transfer edge. */
    private static final double MAX_TRANSFER_WALK_KM = 0.5;

    /** Walking edges cost this many times more than bus edges of the same distance. */
    private static final double WALK_PENALTY_FACTOR = 3.0;

    // ── Cached data ──────────────────────────────────────────────────────────
    private volatile DefaultDirectedWeightedGraph<Integer, DefaultWeightedEdge> cachedGraph;
    private volatile Map<Integer, RouteVertex> nodeMap;
    private volatile Map<String, Integer> vertexLookup;

    // ── RouteVertex record ───────────────────────────────────────────────────
    public record RouteVertex(
            int nodeId,
            int rutaId,
            int vertexIndex,
            double lat,
            double lon,
            String sentido
    ) {}

    // ── Lifecycle ────────────────────────────────────────────────────────────

    @PostConstruct
    void initGraph() {
        rebuildGraph();
    }
    @Transactional
    public DefaultDirectedWeightedGraph<Integer, DefaultWeightedEdge> getGraph() {
        if (cachedGraph == null) rebuildGraph();
        return cachedGraph;
    }
    @Transactional
    public Map<Integer, RouteVertex> getNodeMap() {
        if (nodeMap == null) rebuildGraph();
        return nodeMap;
    }

    // ── Graph construction ───────────────────────────────────────────────────
    @Transactional
    public synchronized void rebuildGraph() {
        log.info("Building directed polyline-vertex graph...");

        DefaultDirectedWeightedGraph<Integer, DefaultWeightedEdge> graph =
                new DefaultDirectedWeightedGraph<>(DefaultWeightedEdge.class);
        Map<Integer, RouteVertex> newNodeMap = new HashMap<>();
        Map<String, Integer> newVertexLookup = new HashMap<>();
        AtomicInteger idCounter = new AtomicInteger(0);

        Map<Integer, List<RouteVertex>> verticesByRuta = new HashMap<>();
        List<Ruta> rutas = rutaRepository.findAllActiveWithCoordenadas();


        for (Ruta ruta : rutas) {

            List<Coordenadas> polyline = ruta.getCoordenadas();
            if (polyline == null || polyline.isEmpty()) continue;

            List<RouteVertex> routeVertices = new ArrayList<>(polyline.size());

            // Create nodes
            for (int i = 0; i < polyline.size(); i++) {
                Coordenadas c = polyline.get(i);
                int nodeId = idCounter.getAndIncrement();

                RouteVertex rv = new RouteVertex(
                        nodeId, ruta.getId_ruta(), i,
                        c.getLatitud(), c.getLongitud(), c.getSentido()
                );

                newNodeMap.put(nodeId, rv);
                newVertexLookup.put(ruta.getId_ruta() + ":" + i, nodeId);
                graph.addVertex(nodeId);
                routeVertices.add(rv);
            }

            // Create bus edges based on directionality
            for (int i = 0; i < routeVertices.size() - 1; i++) {
                RouteVertex a = routeVertices.get(i);
                RouteVertex b = routeVertices.get(i + 1);
                double dist = haversine(a.lat(), a.lon(), b.lat(), b.lon());

                boolean isBidirectional = ruta.isBidirectional();
                String sentidoA = a.sentido();
                String sentidoB = b.sentido();

                // Forward edge: A -> B
                if (canGoForward(sentidoA, sentidoB, isBidirectional)) {
                    DefaultWeightedEdge e = graph.addEdge(a.nodeId(), b.nodeId());
                    if (e != null) graph.setEdgeWeight(e, dist);
                }

                // Backward edge: B -> A
                if (canGoBackward(sentidoA, sentidoB, isBidirectional)) {
                    DefaultWeightedEdge e = graph.addEdge(b.nodeId(), a.nodeId());
                    if (e != null) graph.setEdgeWeight(e, dist);
                }
            }
            verticesByRuta.put(ruta.getId_ruta(), routeVertices);
        }

        // Add transfer edges (Inter-route and Intra-route)
        int transferEdgeCount = 0;
        List<Integer> activeRutaIds = new ArrayList<>(verticesByRuta.keySet());

        for (int i = 0; i < activeRutaIds.size(); i++) {
            for (int j = i; j < activeRutaIds.size(); j++) {
                List<RouteVertex> verticesA = verticesByRuta.get(activeRutaIds.get(i));
                List<RouteVertex> verticesB = verticesByRuta.get(activeRutaIds.get(j));

                if (i != j && !boundingBoxesOverlap(verticesA, verticesB)) continue;

                transferEdgeCount += addTransferEdges(graph, verticesA, verticesB, i == j);
            }
        }

        this.cachedGraph = graph;
        this.nodeMap = Collections.unmodifiableMap(newNodeMap);
        this.vertexLookup = Collections.unmodifiableMap(newVertexLookup);

        log.info("Graph built: {} vertices, {} transfer edges",
                graph.vertexSet().size(), transferEdgeCount);
    }

    private boolean canGoForward(String s1, String s2, boolean routeBidirectional) {
        if (s1 == null || s2 == null) return true; // Default to forward if no metadata
        return (s1.equals("IDA") || s1.equals("AMBOS")) && (s2.equals("IDA") || s2.equals("AMBOS"));
    }

    private boolean canGoBackward(String s1, String s2, boolean routeBidirectional) {
        if (s1 == null || s2 == null) return routeBidirectional;
        return (s1.equals("REGRESO") || s1.equals("AMBOS")) && (s2.equals("REGRESO") || s2.equals("AMBOS"));
    }

    private int addTransferEdges(
            DefaultDirectedWeightedGraph<Integer, DefaultWeightedEdge> graph,
            List<RouteVertex> verticesA,
            List<RouteVertex> verticesB,
            boolean isIntraRoute) {

        int count = 0;
        for (RouteVertex va : verticesA) {
            for (RouteVertex vb : verticesB) {
                if (isIntraRoute && Math.abs(va.vertexIndex() - vb.vertexIndex()) <= 1) continue;

                double dist = haversine(va.lat(), va.lon(), vb.lat(), vb.lon());
                if (dist <= MAX_TRANSFER_WALK_KM) {
                    // Transfer is bidirectional (walking)
                    if (!graph.containsEdge(va.nodeId(), vb.nodeId())) {
                        DefaultWeightedEdge e1 = graph.addEdge(va.nodeId(), vb.nodeId());
                        if (e1 != null) {
                            graph.setEdgeWeight(e1, dist * WALK_PENALTY_FACTOR);
                            count++;
                        }
                    }
                    if (!graph.containsEdge(vb.nodeId(), va.nodeId())) {
                        DefaultWeightedEdge e2 = graph.addEdge(vb.nodeId(), va.nodeId());
                        if (e2 != null) {
                            graph.setEdgeWeight(e2, dist * WALK_PENALTY_FACTOR);
                            count++;
                        }
                    }
                }
            }
        }
        return count;
    }

    public Integer findNearestVertex(double lat, double lon) {
        return nodeMap.values().stream()
                .min(Comparator.comparingDouble(v -> haversine(lat, lon, v.lat(), v.lon())))
                .map(RouteVertex::nodeId)
                .orElse(null);
    }

    private boolean boundingBoxesOverlap(List<RouteVertex> verticesA, List<RouteVertex> verticesB) {
        double buffer = MAX_TRANSFER_WALK_KM * 0.009;
        double[] boxA = boundingBox(verticesA);
        double[] boxB = boundingBox(verticesB);
        return !(boxA[1] + buffer < boxB[0] - buffer || boxA[0] - buffer > boxB[1] + buffer
                || boxA[3] + buffer < boxB[2] - buffer || boxA[2] - buffer > boxB[3] + buffer);
    }

    private double[] boundingBox(List<RouteVertex> vertices) {
        double minLat = Double.MAX_VALUE, maxLat = -Double.MAX_VALUE;
        double minLon = Double.MAX_VALUE, maxLon = -Double.MAX_VALUE;
        for (RouteVertex v : vertices) {
            if (v.lat() < minLat) minLat = v.lat();
            if (v.lat() > maxLat) maxLat = v.lat();
            if (v.lon() < minLon) minLon = v.lon();
            if (v.lon() > maxLon) maxLon = v.lon();
        }
        return new double[]{minLat, maxLat, minLon, maxLon};
    }
}
