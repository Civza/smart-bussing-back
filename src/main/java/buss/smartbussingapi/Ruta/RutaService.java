package buss.smartbussingapi.Ruta;

import buss.smartbussingapi.Coordenadas.Coordenadas;
import buss.smartbussingapi.DTOs.GeoJsonRouteDTO;
import buss.smartbussingapi.commons.exceptions.InvalidDataException;
import buss.smartbussingapi.commons.exceptions.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class RutaService {

    @Autowired
    private final RutaRepository rutaRepository;

    public RutaService(RutaRepository rutaRepository) {
        this.rutaRepository = rutaRepository;
    }

    public Ruta getRutaById(int ruta_id) {
        return rutaRepository.findById(ruta_id)
                .orElseThrow(() -> new NotFoundException("Route with ID " + ruta_id + " not found"));
    }

    public List<Ruta> getAllRutas() {
        return rutaRepository.findAll();
    }

    public List<Coordenadas> getCoordenadasRuta(int ruta_id) {
        Ruta ruta = rutaRepository.findById(ruta_id)
                .orElseThrow(() -> new NotFoundException("Route with ID " + ruta_id + " not found"));
        if (ruta.getCoordenadas().isEmpty()) {
            throw new NotFoundException("No coordinates found for route with ID " + ruta_id);
        }
        return ruta.getCoordenadas();
    }

    public Ruta createNewRouteFromGeoJSON(GeoJsonRouteDTO geoJsonRouteDTO) {
        if(!"route".equals(geoJsonRouteDTO.getProperties().getFeature_type())){
            throw new InvalidDataException("The type of the feature needs to be 'route'");
        }

        // Validate geometry type
        if (!"LineString".equals(geoJsonRouteDTO.getGeometry().getType())) {
            throw new InvalidDataException("GeoJSON geometry type must be 'LineString'");
        }

        // Validate coordinates
        List<List<Double>> coords = geoJsonRouteDTO.getGeometry().getCoordinates();
        if (coords == null || coords.isEmpty()) {
            throw new InvalidDataException("Route must have at least one coordinate");
        }

        //Validation para no pasar el area de Ensenada
        for(List<Double> coor : coords){
            double longitud = coor.get(0);
            double latitud = coor.get(1);

            if (longitud < -180 || longitud > 180) {
                throw new InvalidDataException("Invalid longitude value: " + longitud);
            }
            if (latitud < -90 || latitud > 90) {
                throw new InvalidDataException("Invalid latitude value: " + latitud);
            }
        }

        Ruta ruta = new Ruta();
        ruta.setNombre_ruta(geoJsonRouteDTO.getProperties().getRoute_long_name());
        ruta.setNombre_corto_ruta(geoJsonRouteDTO.getProperties().getRoute_short_name());
        ruta.setColor_ruta(geoJsonRouteDTO.getProperties().getRoute_color());
        ruta.setColor_texto_ruta(geoJsonRouteDTO.getProperties().getRoute_text_color());
        ruta.setTipo_ruta(geoJsonRouteDTO.getProperties().getRoute_type());

        List<Coordenadas> coordenadasList = coords.stream().map(coord -> {
            Coordenadas curr = new Coordenadas();
            curr.setLongitud(coord.get(0));
            curr.setLatitud(coord.get(1));
            List<Ruta> routes = new ArrayList<>();
            routes.add(ruta);
            curr.setRutaList(routes);
            return curr;
        }).toList();

        ruta.setCoordenadas(coordenadasList);
        ruta.setActive(true);
        return rutaRepository.save(ruta);
    }

    /*
    public void agregarCoordenadas(int ruta_id, List<Coordenadas> coordenadas) {
        if (coordenadas == null || coordenadas.isEmpty()) {
            throw new InvalidDataException("Coordinates list cannot be empty");
        }
        Ruta ruta = rutaRepository.findById(ruta_id)
                .orElseThrow(() -> new NotFoundException("Route with ID " + ruta_id + " not found"));

        ruta.getCoordenadas().addAll(coordenadas);
        rutaRepository.save(ruta);
    }

     */
}