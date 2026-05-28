package buss.smartbussingapi.Viaje.CreatingOptimistPath;

import buss.smartbussingapi.DTOs.DirectionsResponse;
import buss.smartbussingapi.DTOs.GeoJsonRoute.GeoJsonRouteGeometry;
import buss.smartbussingapi.Parada.Parada;
import buss.smartbussingapi.Parada.ParadaService;
import buss.smartbussingapi.commons.exceptions.NotFoundException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import static buss.smartbussingapi.commons.Methods.haversine;

@Service
@RequiredArgsConstructor
public class MapboxService {

    @Value("${mapbox.token}")
    private String token;

    @PostConstruct
    void checkTokenLoaded() {
        System.out.println("Mapbox token loaded? " + (token != null && !token.isBlank()));
    }

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private final WebClient webClient = WebClient.create("https://api.mapbox.com");
    private final ParadaService paradaService;

    // Devuelve la parada más cercana dadas las coordenadas del usuario
    public Parada findNearestStop(double lat, double lon) {
        List<Parada> allStops = paradaService.getParadasList();
        if (allStops.isEmpty()) {
            throw new NotFoundException("No hay paradas registradas aún");
        }
        return allStops.stream()
                .min(Comparator.comparingDouble((Parada p) ->
                    haversine(lat, lon,
                            p.getCoordenadasParada().getLatitud(),
                            p.getCoordenadasParada().getLongitud()
                    )
                ))
                .orElseThrow(() -> new NotFoundException("No se encontró ninguna parada cercana"));
    }

    /**
     * Caminata entre dos coordenadas cualesquiera.
     */
    public DirectionsResponse getWalkingDirections(double startLat, double startLon, double endLat, double endLon) {
        String coordinates = startLon + "," + startLat + ";" + endLon + "," + endLat;
        String response = callMapboxWalking(coordinates);
        return parseResponse(response, null);
    }

    /**
     * Step 1: Caminata desde las coordenadas del usuario hasta la parada de abordaje.
     */
    public DirectionsResponse getWalkingDirections(double userLat, double userLon, Parada parada) {
        return getWalkingDirections(userLat, userLon,
                parada.getCoordenadasParada().getLatitud(),
                parada.getCoordenadasParada().getLongitud());
    }

    /**
     * Step 5: Caminata desde la parada de bajada hasta el destino real del usuario.
     */
    public DirectionsResponse getWalkingDirections(Parada parada, double destLat, double destLon) {
        return getWalkingDirections(
                parada.getCoordenadasParada().getLatitud(),
                parada.getCoordenadasParada().getLongitud(),
                destLat, destLon);
    }

    private String callMapboxWalking(String coordinates) {
        return webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/directions/v5/mapbox/walking/{coordinates}")
                        .queryParam("access_token", token)
                        .queryParam("geometries", "geojson")
                        .queryParam("steps", "true")
                        .queryParam("language", "es")
                        .build(coordinates))
                .retrieve()
                .onStatus(HttpStatusCode::isError, resp ->
                        resp.bodyToMono(String.class).map(body ->
                                new RuntimeException("Mapbox error " + resp.statusCode() + ": " + body)))
                .bodyToMono(String.class)
                .block();
    }

    private DirectionsResponse parseResponse(String json, Parada parada) {
        try {
            JsonNode root = MAPPER.readTree(json);
            JsonNode routes = root.path("routes");

            if (routes.isEmpty()) {
                throw new NotFoundException("Mapbox no encontró una ruta para las coordenadas dadas");
            }

            JsonNode travel = routes.get(0);
            double distance = travel.path("distance").asDouble();
            double duration = travel.path("duration").asDouble();

            JsonNode geometry = travel.path("geometry");

            List<String> steps = new ArrayList<>();
            travel.path("legs").get(0).path("steps").forEach(step ->
                    steps.add(step.path("maneuver").path("instruction").asText())
            );

            GeoJsonRouteGeometry geoJson = new GeoJsonRouteGeometry();
            geoJson.setType(geometry.path("type").asText());

            List<List<Double>> coordinates = new ArrayList<>();
            geometry.path("coordinates").forEach(coord -> {
                List<Double> point = List.of(
                        coord.get(0).asDouble(),  // longitud
                        coord.get(1).asDouble()   // latitud
                );
                coordinates.add(point);
            });
            geoJson.setCoordinates(coordinates);

            return DirectionsResponse.builder()
                    .paradaDestino(parada)
                    .distanceMeters(distance)
                    .timeSeconds(duration)
                    .geoJson(geoJson)
                    .instructions(steps)
                    .build();

        } catch (NotFoundException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("Error al parsear la respuesta de Mapbox: " + e.getMessage());
        }
    }
}
