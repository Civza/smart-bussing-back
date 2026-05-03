package buss.smartbussingapi.DTOs.GeoJsonStops;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class GeoJsonStopDTO {
    private String type;
    private String id;
    private GeoJsonStopGeometry geometry;
    private GeoJsonStopProperties properties;
}
