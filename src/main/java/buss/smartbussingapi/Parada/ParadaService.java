package buss.smartbussingapi.Parada;

import buss.smartbussingapi.Coordenadas.Coordenadas;
import buss.smartbussingapi.DTOs.GeoJsonStops.GeoJsonStopDTO;
import buss.smartbussingapi.Ruta.Ruta;
import buss.smartbussingapi.Ruta.RutaRepository;
import buss.smartbussingapi.commons.exceptions.InvalidDataException;
import buss.smartbussingapi.commons.exceptions.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ParadaService {

    @Autowired
    private final ParadaRepository paradaRepository;
    private final RutaRepository rutaRepository;

    public ParadaService(ParadaRepository paradaRepository, RutaRepository rutaRepository) {
        this.paradaRepository = paradaRepository;
        this.rutaRepository = rutaRepository;
    }

    public List<Parada> getParadasList(){
        return paradaRepository.findAll();
    }

    public Parada getParadaById(int parada_id) {
        return paradaRepository.findById(parada_id)
                .orElseThrow(() -> new NotFoundException("Parada with ID " + parada_id + " not found"));
    }

    public Parada addParada(GeoJsonStopDTO geoJsonStopDTO){
        if(!"stop".equals(geoJsonStopDTO.getProperties().getFeature_type())){
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
        parada.setNombre_parada(geoJsonStopDTO.getProperties().getStop_name());
        parada.setDescripcion_parada(geoJsonStopDTO.getProperties().getStop_description());

        Coordenadas newCoor = new Coordenadas();
        newCoor.setLatitud(latitud);
        newCoor.setLongitud(longitud);

        parada.setCoordenadas_parada(newCoor);

        if(geoJsonStopDTO.getProperties().getRoutes_names() != null){
            for(String routeName : geoJsonStopDTO.getProperties().getRoutes_names()){
                Ruta curr_route = rutaRepository.findRutaByNombre_ruta(routeName);
                if(curr_route == null){
                    throw new NotFoundException("The route with name : " + routeName + "doesnt exist");
                }
                curr_route.getParadas().add(parada);
            }
        }

        return paradaRepository.save(parada);
    }

}
