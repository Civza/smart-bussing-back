package buss.smartbussingapi.Viaje.CreatingOptimistPath;

import buss.smartbussingapi.Coordenadas.Coordenadas;
import buss.smartbussingapi.DTOs.GeoJsonRoute.GeoJsonRouteGeometry;
import buss.smartbussingapi.Parada.Parada;
import buss.smartbussingapi.Ruta.Ruta;
import buss.smartbussingapi.commons.exceptions.InvalidDataException;
import buss.smartbussingapi.commons.exceptions.NotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

import static buss.smartbussingapi.commons.Methods.haversine;

@Service
@RequiredArgsConstructor
public class BuildBusGeoJson {

    public GeoJsonRouteGeometry buildBusGeoJson(List<Parada> paradasRuta) {

        if (paradasRuta == null || paradasRuta.size() < 2) {
            throw new InvalidDataException("Se necesitan al menos 2 paradas para construir el segmento de bus");
        }

        // Deriva la ruta real a partir de las paradas del segmento
        Ruta ruta = findCommonRoute(paradasRuta.get(0), paradasRuta.get(paradasRuta.size() - 1));
        List<Coordenadas> trayectoCompleto = ruta.getCoordenadas();

        // Índice en el trayecto más cercano a la parada de abordaje
        int idxOrigen = indiceMasCercano(
                paradasRuta.get(0).getCoordenadasParada(), trayectoCompleto
        );

        // Índice en el trayecto más cercano a la parada de descenso
        int idxDestino = indiceMasCercano(
                paradasRuta.get(paradasRuta.size() - 1).getCoordenadasParada(), trayectoCompleto
        );

        // Asegura que origen < destino
        int desde = Math.min(idxOrigen, idxDestino);
        int hasta = Math.max(idxOrigen, idxDestino);

        List<List<Double>> coordinates = trayectoCompleto.subList(desde, hasta + 1)
                .stream()
                .map(c -> List.of(c.getLongitud(), c.getLatitud()))
                .collect(Collectors.toList());

        return new GeoJsonRouteGeometry("LineString", coordinates);
    }

    /**
     * Busca la primera Ruta que tenga en común la parada de origen y la de destino.
     * Si no existe ruta compartida (caso de transbordo aún no soportado) lanza NotFoundException.
     */
    private Ruta findCommonRoute(Parada origen, Parada destino) {
        List<Integer> idsRutasOrigen = origen.getRutas().stream()
                .map(Ruta::getIdRuta)
                .toList();

        return destino.getRutas().stream()
                .filter(r -> idsRutasOrigen.contains(r.getIdRuta()))
                .findFirst()
                .orElseThrow(() -> new NotFoundException(
                        "No se encontró una ruta común entre la parada '" + origen.getNombreParada()
                        + "' y '" + destino.getNombreParada() + "'"
                ));
    }

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
