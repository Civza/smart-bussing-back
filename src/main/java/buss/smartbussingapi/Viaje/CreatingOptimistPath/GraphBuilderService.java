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
    private static final double MAX_TRANSFER_WALK_KM = 0.35;

    /** Walking edges cost this many times more than bus edges of the same distance. */
    public static final double WALK_PENALTY_FACTOR = 3.0;

    /** Only initiate transfer edges from every N-th vertex to reduce graph density. */
    private static final int TRANSFER_SAMPLING_INTERVAL = 5;
    
    /** Penalty (in km) added to every inter-route transfer edge to discourage switching buses. */
    private static final double TRANSFER_PENALTY_KM = 3.5;

    /** Penalty (in km) for transfers within the same route (e.g. skipping a loop). */
    private static final double INTRA_ROUTE_PENALTY_KM = 2.5;

    /** Grid cell size in degrees (approx 500m). */
    private static final double GRID_CELL_SIZE = 0.0045;

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
        log.info("Adding transfer edges using spatial index...");
        int transferEdgeCount = addTransferEdgesOptimized(graph, newNodeMap, verticesByRuta);

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

    private int addTransferEdgesOptimized(
            DefaultDirectedWeightedGraph<Integer, DefaultWeightedEdge> graph,
            Map<Integer, RouteVertex> allNodes,
            Map<Integer, List<RouteVertex>> verticesByRuta) {

        int count = 0;
        // 1. Build Spatial Index
        Map<Long, List<RouteVertex>> grid = new HashMap<>(allNodes.size());
        for (RouteVertex v : allNodes.values()) {
            long key = getGridKey(v.lat(), v.lon());
            grid.computeIfAbsent(key, k -> new ArrayList<>()).add(v);
        }

        // 2. Add edges using spatial lookup and pruning
        for (List<RouteVertex> routeList : verticesByRuta.values()) {
            for (int i = 0; i < routeList.size(); i += TRANSFER_SAMPLING_INTERVAL) {
                RouteVertex va = routeList.get(i);

                // Find nearest vertex in each other route within distance
                Map<Integer, Double> minDists = new HashMap<>();
                Map<Integer, RouteVertex> bestNeighbors = new HashMap<>();

                long cellX = (long) (va.lat() / GRID_CELL_SIZE);
                long cellY = (long) (va.lon() / GRID_CELL_SIZE);

                for (long dx = -1; dx <= 1; dx++) {
                    for (long dy = -1; dy <= 1; dy++) {
                        List<RouteVertex> cellNodes = grid.get(getGridKeyFromCells(cellX + dx, cellY + dy));
                        if (cellNodes == null) continue;

                        for (RouteVertex vb : cellNodes) {
                            // Pruning: Skip same node or immediate neighbors in same route
                            if (va.rutaId() == vb.rutaId() && Math.abs(va.vertexIndex() - vb.vertexIndex()) <= 2) continue;

                            double dist = haversine(va.lat(), va.lon(), vb.lat(), vb.lon());
                            if (dist <= MAX_TRANSFER_WALK_KM) {
                                if (dist < minDists.getOrDefault(vb.rutaId(), Double.MAX_VALUE)) {
                                    minDists.put(vb.rutaId(), dist);
                                    bestNeighbors.put(vb.rutaId(), vb);
                                }
                            }
                        }
                    }
                }

                // Add edges to the best neighbor found for each route
                for (Map.Entry<Integer, RouteVertex> entry : bestNeighbors.entrySet()) {
                    RouteVertex vb = entry.getValue();
                    double dist = minDists.get(entry.getKey());

                    // Apply appropriate penalty
                    double weight = (dist * WALK_PENALTY_FACTOR);
                    if (va.rutaId() != vb.rutaId()) {
                        weight += TRANSFER_PENALTY_KM;
                    } else {
                        weight += INTRA_ROUTE_PENALTY_KM;
                    }

                    // Transfer is bidirectional (walking)
                    if (!graph.containsEdge(va.nodeId(), vb.nodeId())) {
                        DefaultWeightedEdge e1 = graph.addEdge(va.nodeId(), vb.nodeId());
                        if (e1 != null) {
                            graph.setEdgeWeight(e1, weight);
                            count++;
                        }
                    }
                    if (!graph.containsEdge(vb.nodeId(), va.nodeId())) {
                        DefaultWeightedEdge e2 = graph.addEdge(vb.nodeId(), va.nodeId());
                        if (e2 != null) {
                            graph.setEdgeWeight(e2, weight);
                            count++;
                        }
                    }
                }
            }
        }
        return count;
    }

    private long getGridKey(double lat, double lon) {
        return getGridKeyFromCells((long) (lat / GRID_CELL_SIZE), (long) (lon / GRID_CELL_SIZE));
    }

    private long getGridKeyFromCells(long x, long y) {
        return (x << 32) | (y & 0xFFFFFFFFL);
    }

    public Integer findNearestVertex(double lat, double lon) {
        return nodeMap.values().stream()
                .min(Comparator.comparingDouble(v -> haversine(lat, lon, v.lat(), v.lon())))
                .map(RouteVertex::nodeId)
                .orElse(null);
    }

    public List<RouteVertex> findNearestVertices(double lat, double lon, int limit, double maxDistKm) {
        return nodeMap.values().stream()
                .filter(v -> haversine(lat, lon, v.lat(), v.lon()) <= maxDistKm)
                .sorted(Comparator.comparingDouble(v -> haversine(lat, lon, v.lat(), v.lon())))
                .limit(limit)
                .toList();
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
