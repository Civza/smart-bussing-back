package buss.smartbussingapi.Viaje.CreatingOptimistPath;

import buss.smartbussingapi.Coordenadas.Coordenadas;
import buss.smartbussingapi.DTOs.GeoJsonRoute.GeoJsonRouteGeometry;
import buss.smartbussingapi.Parada.Parada;
import buss.smartbussingapi.Parada.ParadaRepository;
import buss.smartbussingapi.Ruta.Ruta;
import buss.smartbussingapi.Ruta.RutaRepository;
import buss.smartbussingapi.commons.exceptions.NotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static buss.smartbussingapi.commons.Methods.haversine;

@Service
@RequiredArgsConstructor
public class BuildBusGeoJson {

    private final RutaRepository rutaRepository;

    public GeoJsonRouteGeometry buildBusGeoJson(List<Parada> paradasRuta, int ruta_id) {

        //In the future has no manage multiples routes with the transbords
        Optional<Ruta> ruta = rutaRepository.findById(ruta_id);

        if(ruta.isEmpty()){
            throw new NotFoundException("The route with Id : " + ruta_id + "wasnt found");
        }

        List<Coordenadas> trayectoCompleto = ruta.get().getCoordenadas();
        // Índice en el trayecto más cercano a la parada de abordaje
        int idxOrigen = indiceMasCercano(
                paradasRuta.get(0).getCoordenadas_parada(), trayectoCompleto
        );

        // Índice en el trayecto más cercano a la parada de descenso
        int idxDestino = indiceMasCercano(
                paradasRuta.get(paradasRuta.size() - 1).getCoordenadas_parada(), trayectoCompleto
        );

        // Asegura que origen < destino
        int desde = Math.min(idxOrigen, idxDestino);
        int hasta = Math.max(idxOrigen, idxDestino);

        // Recorta el trayecto solo entre esos dos puntos
        List<Coordenadas> trayectoRecortado = trayectoCompleto.subList(desde, hasta + 1);

        List<List<Double>> coordinates = trayectoCompleto.subList(desde, hasta + 1)
                .stream()
                .map(c -> List.of(c.getLongitud(), c.getLatitud()))
                .collect(Collectors.toList());


        return new GeoJsonRouteGeometry("LineString", coordinates);
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
