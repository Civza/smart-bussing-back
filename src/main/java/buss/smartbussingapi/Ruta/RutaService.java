package buss.smartbussingapi.Ruta;

import buss.smartbussingapi.Coordenadas.Coordenadas;
import buss.smartbussingapi.DTOs.RutaRequestDTO;
import buss.smartbussingapi.commons.exceptions.InvalidDataException;
import buss.smartbussingapi.commons.exceptions.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import com.fasterxml.jackson.databind.JsonNode;
import buss.smartbussingapi.DTOs.GeoJsonFeatureCollectionDTO;
import buss.smartbussingapi.Parada.Parada;

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

    public Ruta agregarRuta(RutaRequestDTO ruta) {
        //TODO - NEW LOGIC COMING SOON (Validators)
        return rutaRepository.save(new Ruta(null, ruta.nombre_ruta(), ruta.nombre_corto_ruta(), ruta.color_ruta(), ruta.color_texto_ruta(), ruta.tipo_ruta(), ruta.horario_ruta(), ruta.active(), null, null, null));
    }

    public void agregarCoordenadas(int ruta_id, List<Coordenadas> coordenadas) {
        if (coordenadas == null || coordenadas.isEmpty()) {
            throw new InvalidDataException("Coordinates list cannot be empty");
        }
        Ruta ruta = rutaRepository.findById(ruta_id)
                .orElseThrow(() -> new NotFoundException("Route with ID " + ruta_id + " not found"));

        ruta.getCoordenadas().addAll(coordenadas);
        rutaRepository.save(ruta);
    }

    public Ruta agregarRutaDesdeGeoJson(GeoJsonFeatureCollectionDTO payload) {
        if (payload == null || payload.features() == null) {
            throw new InvalidDataException("Invalid GeoJSON payload");
        }

        JsonNode routeFeature = null;
        List<JsonNode> stopFeatures = new ArrayList<>();

        for (JsonNode feature : payload.features()) {
            JsonNode properties = feature.get("properties");
            if (properties != null && properties.has("feature_type")) {
                String type = properties.get("feature_type").asText();
                if ("route".equals(type)) {
                    routeFeature = feature;
                } else if ("stop".equals(type)) {
                    stopFeatures.add(feature);
                }
            }
        }

        if (routeFeature == null) {
            throw new InvalidDataException("No route feature found in GeoJSON");
        }

        JsonNode routeProps = routeFeature.get("properties");
        String nombreRuta = routeProps.has("route_long_name") ? routeProps.get("route_long_name").asText() : "";
        String nombreCortoRuta = routeProps.has("route_short_name") ? routeProps.get("route_short_name").asText() : "";
        String colorRuta = routeProps.has("route_color") ? routeProps.get("route_color").asText() : "#000000";
        String colorTextoRuta = routeProps.has("route_text_color") ? routeProps.get("route_text_color").asText() : "#FFFFFF";
        String tipoRuta = routeProps.has("route_type") ? routeProps.get("route_type").asText() : "microbus";

        Ruta ruta = new Ruta();
        ruta.setNombre_ruta(nombreRuta);
        ruta.setNombre_corto_ruta(nombreCortoRuta);
        ruta.setColor_ruta(colorRuta);
        ruta.setColor_texto_ruta(colorTextoRuta);
        ruta.setTipo_ruta(tipoRuta);
        ruta.setActive(true);

        JsonNode routeGeom = routeFeature.get("geometry");
        if (routeGeom != null && routeGeom.has("coordinates")) {
            JsonNode coordinatesNode = routeGeom.get("coordinates");
            if (coordinatesNode.isArray()) {
                List<Coordenadas> rutaCoordenadas = new ArrayList<>();
                for (JsonNode coordPair : coordinatesNode) {
                    if (coordPair.isArray() && coordPair.size() >= 2) {
                        double lon = coordPair.get(0).asDouble();
                        double lat = coordPair.get(1).asDouble();
                        Coordenadas coord = new Coordenadas();
                        coord.setLongitud(lon);
                        coord.setLatitud(lat);
                        rutaCoordenadas.add(coord);
                    }
                }
                ruta.setCoordenadas(rutaCoordenadas);
            }
        }

        List<Parada> paradas = new ArrayList<>();
        for (JsonNode stopFeature : stopFeatures) {
            JsonNode stopProps = stopFeature.get("properties");
            String nombreParada = stopProps.has("stop_name") ? stopProps.get("stop_name").asText() : "";
            String descripcionParada = stopProps.has("stop_description") ? stopProps.get("stop_description").asText() : "";

            Parada parada = new Parada();
            parada.setNombre_parada(nombreParada);
            parada.setDescripcion_parada(descripcionParada);
            parada.setZona_parada("");
            parada.setTiempo_Espera(0);

            JsonNode stopGeom = stopFeature.get("geometry");
            if (stopGeom != null && stopGeom.has("coordinates")) {
                JsonNode coordPair = stopGeom.get("coordinates");
                if (coordPair.isArray() && coordPair.size() >= 2) {
                    double lon = coordPair.get(0).asDouble();
                    double lat = coordPair.get(1).asDouble();
                    Coordenadas coord = new Coordenadas();
                    coord.setLongitud(lon);
                    coord.setLatitud(lat);
                    parada.setCoordenadas_parada(coord);
                }
            }
            paradas.add(parada);
        }

        ruta.setParadas(paradas);

        return rutaRepository.save(ruta);
    }
}