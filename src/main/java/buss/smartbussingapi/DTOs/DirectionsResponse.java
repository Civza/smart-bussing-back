package buss.smartbussingapi.DTOs;

import buss.smartbussingapi.DTOs.GeoJsonRoute.GeoJsonRouteGeometry;
import buss.smartbussingapi.Parada.Parada;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class DirectionsResponse {
    private Parada paradaDestino;
    private double distanceMeters;
    private double timeSeconds;
    private GeoJsonRouteGeometry geoJson;
    private List<String> instructions;
}
