package buss.smartbussingapi.Ruta;

import buss.smartbussingapi.Coordenadas.Coordenadas;
import buss.smartbussingapi.DTOs.GeoJsonRoute.GeoJsonRouteDTO;
import buss.smartbussingapi.Viaje.CreatingOptimistPath.GraphBuilderService;
import buss.smartbussingapi.commons.exceptions.InvalidDataException;
import buss.smartbussingapi.commons.exceptions.NotFoundException;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import com.fasterxml.jackson.databind.JsonNode;
import buss.smartbussingapi.DTOs.GeoJsonFeatureCollectionDTO;
import buss.smartbussingapi.Parada.Parada;

@Service
public class RutaService {

    private final RutaRepository rutaRepository;
    private final GraphBuilderService graphBuilderService;
    @SuppressWarnings("EI_EXPOSE_REP2") // This is a Java bean that needs to be injected with the reference to the object
    public RutaService(RutaRepository rutaRepository, GraphBuilderService graphBuilderService) {
        this.rutaRepository = rutaRepository;
        this.graphBuilderService = graphBuilderService;
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
        if (!"route".equals(geoJsonRouteDTO.getProperties().getFeatureType())) {
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
        for (List<Double> coor : coords) {
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
        ruta.setNombreRuta(geoJsonRouteDTO.getProperties().getRouteLongName());
        ruta.setNombreCortoRuta(geoJsonRouteDTO.getProperties().getRouteShortName());
        ruta.setColorRuta(geoJsonRouteDTO.getProperties().getRouteColor());
        ruta.setColorTextoRuta(geoJsonRouteDTO.getProperties().getRouteTextColor());
        String tipoRutaStr = geoJsonRouteDTO.getProperties().getRouteType();
        ruta.setTipoRuta(tipoRutaStr != null ? RutaType.valueOf(tipoRutaStr.toUpperCase()) : RutaType.URBANA);

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
        Ruta saved = rutaRepository.save(ruta);
        graphBuilderService.rebuildGraph();
        return saved;
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
    
    /*


     */

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
        String tipoRutaStr = routeProps.has("route_type") ? routeProps.get("route_type").asText() : "URBANA";
        RutaType tipoRuta = RutaType.valueOf(tipoRutaStr.toUpperCase());
        boolean bidirectional = !routeProps.has("bidirectional") || routeProps.get("bidirectional").asBoolean();
        String defaultSentido = routeProps.has("sentido") ? routeProps.get("sentido").asText() : "AMBOS";

        Ruta ruta = new Ruta();
        ruta.setNombreRuta(nombreRuta);
        ruta.setNombreCortoRuta(nombreCortoRuta);
        ruta.setColorRuta(colorRuta);
        ruta.setColorTextoRuta(colorTextoRuta);
        ruta.setTipoRuta(tipoRuta);
        ruta.setBidirectional(bidirectional);
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
                        // If coordinates have a 3rd element, treat it as sentido (custom)
                        if (coordPair.size() >= 3) {
                            coord.setSentido(coordPair.get(2).asText());
                        } else {
                            coord.setSentido(defaultSentido);
                        }
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
            parada.setNombreParada(nombreParada);
            parada.setDescripcionParada(descripcionParada);


            JsonNode stopGeom = stopFeature.get("geometry");
            if (stopGeom != null && stopGeom.has("coordinates")) {
                JsonNode coordPair = stopGeom.get("coordinates");
                if (coordPair.isArray() && coordPair.size() >= 2) {
                    double lon = coordPair.get(0).asDouble();
                    double lat = coordPair.get(1).asDouble();
                    Coordenadas coord = new Coordenadas();
                    coord.setLongitud(lon);
                    coord.setLatitud(lat);
                    parada.setCoordenadasParada(coord);
                }
            }
            paradas.add(parada);
        }

        ruta.setParadas(paradas);

        Ruta saved = rutaRepository.save(ruta);
        graphBuilderService.rebuildGraph();
        return saved;
    }
}