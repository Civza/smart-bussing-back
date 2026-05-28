package buss.smartbussingapi.DTOs;

import buss.smartbussingapi.DTOs.GeoJsonRoute.GeoJsonRouteGeometry;
import buss.smartbussingapi.Parada.Parada;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@Builder
public class DirectionsResponse {
    private Parada paradaDestino;
    private double distanceMeters;
    private double timeSeconds;
    private GeoJsonRouteGeometry geoJson;
    private List<String> instructions;
}
