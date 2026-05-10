package buss.smartbussingapi.Viaje.CreatingOptimistPath;

import buss.smartbussingapi.Coordenadas.Coordenadas;
import buss.smartbussingapi.Parada.Parada;
import buss.smartbussingapi.Ruta.Ruta;
import buss.smartbussingapi.Ruta.RutaRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jgrapht.graph.DefaultWeightedEdge;
import org.jgrapht.graph.SimpleWeightedGraph;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import static buss.smartbussingapi.commons.Methods.haversine;

@Service
@RequiredArgsConstructor
@Slf4j
public class GraphBuilderService {

    private final RutaRepository rutaRepository;

    /** Cached graph singleton — built once at startup, rebuilt on demand. */
    private volatile SimpleWeightedGraph<Integer, DefaultWeightedEdge> cachedGraph;

    @PostConstruct
    void initGraph() {
        rebuildGraph();
    }

    /**
     * Returns the cached graph. If it hasn't been built yet (shouldn't happen
     * after @PostConstruct), it builds it on-the-fly as a safety fallback.
     */
    public SimpleWeightedGraph<Integer, DefaultWeightedEdge> getGraph() {
        if (cachedGraph == null) {
            rebuildGraph();
        }
        return cachedGraph;
    }

    /**
     * Rebuilds the graph from the current DB state and swaps the cached reference.
     * Call this whenever routes or stops are created, updated, or deleted.
     */
    public synchronized void rebuildGraph() {
        log.info("Building bus-stop graph from DB...");
        SimpleWeightedGraph<Integer, DefaultWeightedEdge> graph =
                new SimpleWeightedGraph<>(DefaultWeightedEdge.class);

        List<Ruta> rutas = rutaRepository.findAll();

        for (Ruta ruta : rutas) {
            List<Coordenadas> trayecto = ruta.getCoordenadas();
            List<Parada> paradas = orderStopsByNearest(ruta.getParadas(), trayecto);

            for (Parada p : paradas) {
                graph.addVertex(p.getId_parada());
            }

            for (int i = 0; i < paradas.size() - 1; i++) {
                Parada a = paradas.get(i);
                Parada b = paradas.get(i + 1);

                DefaultWeightedEdge edge = graph.addEdge(
                        a.getId_parada(), b.getId_parada()
                );

                if (edge != null) {
                    double dist = haversine(
                            a.getCoordenadas_parada().getLatitud(),
                            a.getCoordenadas_parada().getLongitud(),
                            b.getCoordenadas_parada().getLatitud(),
                            b.getCoordenadas_parada().getLongitud()
                    );
                    graph.setEdgeWeight(edge, dist);
                }
            }
        }

        this.cachedGraph = graph;
        log.info("Bus-stop graph built: {} vertices, {} edges",
                graph.vertexSet().size(), graph.edgeSet().size());
    }

    // Ordena las paradas según su posición más cercana en el polyline
    private List<Parada> orderStopsByNearest(List<Parada> paradas, List<Coordenadas> trayecto) {
        return paradas.stream()
                .sorted(Comparator.comparingInt(parada ->
                        indiceMasCercano(parada.getCoordenadas_parada(), trayecto)
                ))
                .collect(Collectors.toList());
    }

    // Devuelve el índice de la coordenada del trayecto más cercana a una parada
    private int indiceMasCercano(Coordenadas parada, List<Coordenadas> trayecto) {
        int indiceMenor = 0;
        double distanciaMinima = Double.MAX_VALUE;

        for (int i = 0; i < trayecto.size(); i++) {
            double dist = haversine(
                    parada.getLatitud(), parada.getLongitud(),
                    trayecto.get(i).getLatitud(), trayecto.get(i).getLongitud()
            );
            if (dist < distanciaMinima) {
                distanciaMinima = dist;
                indiceMenor = i;
            }
        }

        return indiceMenor;
    }

}

