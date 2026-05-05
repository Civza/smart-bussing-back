package buss.smartbussingapi.Viaje.CreatingOptimistPath;

import buss.smartbussingapi.DTOs.DirectionsResponse;
import buss.smartbussingapi.Parada.Parada;
import buss.smartbussingapi.Parada.ParadaService;
import buss.smartbussingapi.commons.exceptions.NotFoundException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
public class MapboxService {

    @Value("${mapbox.token}")
    private String token;

    private final WebClient webClient = WebClient.create("https://api.mapbox.com");

    @Autowired
    private final ParadaService paradaService;

    public MapboxService(ParadaService paradaService){
        this.paradaService = paradaService;
    }

    //1. Devuelve la parada mas corta dada las coordenadas del usuario
    public Parada findNearestStop(double lat, double lon){
        List<Parada> allStops = paradaService.getParadasList();
        if(allStops == null || allStops.isEmpty()){
            throw new NotFoundException("There no any stops register yet");
        }
        return allStops.stream()
                .min(Comparator.comparingDouble( p ->
                    haversine(lat, lon ,
                            p.getCoordenadas_parada().getLatitud(),
                            p.getCoordenadas_parada().getLongitud()
                    )
                ))
                .orElseThrow( () -> new RuntimeException("Something failed looking for stops"));
    }


    public DirectionsResponse getWalkingDirections(double userLat, double userLon, Parada parada){
        double stopLat = parada.getCoordenadas_parada().getLatitud();
        double stopLon = parada.getCoordenadas_parada().getLongitud();

        String coordinates = userLon + "," + userLat + ";" + stopLon + "," + stopLat;

        String response = webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/directions/v5/mapbox/walking/{coordinates}")
                        .queryParam("access_token", token)
                        .queryParam("geometries", "geojson")
                        .queryParam("steps", "true")
                        .queryParam("language", "es")
                        .build(coordinates))
                .retrieve()
                .bodyToMono(String.class)
                .block();

        return parseResponse(response, parada);
    }

    //3. Parse to send to frontend
    private DirectionsResponse parseResponse(String json, Parada parada){
        try{
            ObjectMapper mapper = new ObjectMapper();
            JsonNode root = mapper.readTree(json);
            JsonNode travel = root.path("routes").get(0);

            double distance = travel.path("distance").asDouble();
            double duration = travel.path("duration").asDouble();

            JsonNode geometry = travel.path("geometry");

            List<String> steps = new ArrayList<>();
            travel.path("legs").get(0).path("steps").forEach(step ->
                    steps.add(step.path("maneuver").path("instruction").asText())
            );

            return DirectionsResponse.builder()
                    .paradaDestino(parada)
                    .distanceMeters(distance)
                    .timeSeconds(duration)
                    .geoJson(geometry.toString())
                    .instructions(steps)
                    .build();

        } catch (Exception e){
            throw new RuntimeException("Error parsing the response");
        }
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
