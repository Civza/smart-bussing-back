package buss.smartbussingapi.Parada;

import buss.smartbussingapi.Coordenadas.Coordenadas;
import buss.smartbussingapi.DTOs.GeoJsonStops.GeoJsonStopDTO;
import buss.smartbussingapi.Ruta.Ruta;
import buss.smartbussingapi.Ruta.RutaRepository;
import buss.smartbussingapi.Viaje.CreatingOptimistPath.GraphBuilderService;
import buss.smartbussingapi.commons.exceptions.InvalidDataException;
import buss.smartbussingapi.commons.exceptions.NotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ParadaService {


    private final ParadaRepository paradaRepository;
    private final RutaRepository rutaRepository;
    private final GraphBuilderService graphBuilderService;

    // This is a Java bean that needs to be injected with the reference to the object
    @SuppressWarnings("EI_EXPOSE_REP2")
    public ParadaService(ParadaRepository paradaRepository,
                         RutaRepository rutaRepository,
                         GraphBuilderService graphBuilderService) {
        this.paradaRepository = paradaRepository;
        this.rutaRepository = rutaRepository;
        this.graphBuilderService = graphBuilderService;
    }

    public List<Parada> getParadasList() {
        return paradaRepository.findAll();
    }

    public Parada getParadaById(int paradaId) {
        return paradaRepository.findById(paradaId)
                .orElseThrow(() -> new NotFoundException("Parada with ID " + paradaId + " not found"));
    }

    public Parada addParada(GeoJsonStopDTO geoJsonStopDTO) {
        if (!"stop".equals(geoJsonStopDTO.getProperties().getFeatureType())) {
            throw new InvalidDataException("The type of the feature needs to be 'route'");
        }

        // Validate geometry type
        if (!"Point".equals(geoJsonStopDTO.getGeometry().getType())) {
            throw new InvalidDataException("GeoJSON geometry type must be 'LineString'");
        }

        // Validate coordinates
        List<Double> coor = geoJsonStopDTO.getGeometry().getCoordinates();
        if (coor == null || coor.isEmpty()) {
            throw new InvalidDataException("Route must have at least one coordinate");
        }

        //Validation para no pasar el area de Ensenada
        double longitud = coor.get(0);
        double latitud = coor.get(1);

        if (longitud < -180 || longitud > 180) {
            throw new InvalidDataException("Invalid longitude value: " + longitud);
        }
        if (latitud < -90 || latitud > 90) {
            throw new InvalidDataException("Invalid latitude value: " + latitud);
        }

        Parada parada = new Parada();
        parada.setNombreParada(geoJsonStopDTO.getProperties().getStopName());
        parada.setDescripcionParada(geoJsonStopDTO.getProperties().getStopDescription());

        Coordenadas newCoor = new Coordenadas();
        newCoor.setLatitud(latitud);
        newCoor.setLongitud(longitud);

        parada.setCoordenadasParada(newCoor);

        if (geoJsonStopDTO.getProperties().getRoutesNames() != null) {
            for (String routeName : geoJsonStopDTO.getProperties().getRoutesNames()) {
                Ruta currRoute = rutaRepository.findRutaByNombreRuta(routeName);
                if (currRoute == null) {
                    throw new NotFoundException("The route with name : " + routeName + "doesnt exist");
                }
                currRoute.getParadas().add(parada);
            }
        }

        Parada saved = paradaRepository.save(parada);
        graphBuilderService.rebuildGraph();
        return saved;
    }

}
