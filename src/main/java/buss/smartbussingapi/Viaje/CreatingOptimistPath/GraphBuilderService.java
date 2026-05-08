package buss.smartbussingapi.Viaje.CreatingOptimistPath;

import buss.smartbussingapi.Coordenadas.Coordenadas;
import buss.smartbussingapi.Parada.Parada;
import buss.smartbussingapi.Ruta.Ruta;
import buss.smartbussingapi.Ruta.RutaRepository;
import lombok.RequiredArgsConstructor;
import org.jgrapht.graph.DefaultWeightedEdge;
import org.jgrapht.graph.SimpleWeightedGraph;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class GraphBuilderService {

    private final RutaRepository rutaRepository;

    public SimpleWeightedGraph<Integer, DefaultWeightedEdge> buildGraph(){
        SimpleWeightedGraph<Integer, DefaultWeightedEdge> graph = new SimpleWeightedGraph<>(DefaultWeightedEdge.class);

        List<Ruta> rutas = rutaRepository.findAll();

        for(Ruta ruta : rutas){
            List<Coordenadas> trayecto = ruta.getCoordenadas();
            List<Parada> paradas = orderStopsByNearest(ruta.getParadas(),trayecto);

            for (Parada p : paradas){
                graph.addVertex(p.getId_parada());
            }

            for (int i = 0; i < paradas.size() - 1; i++){
                Parada a = paradas.get(i);
                Parada b = paradas.get(i + 1);

                DefaultWeightedEdge edge = graph.addEdge(
                        a.getId_parada(),b.getId_parada()
                );

                if(edge != null){
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

        return graph;

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
