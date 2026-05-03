package buss.smartbussingapi.DTOs.GeoJsonRoute;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class GeoJsonRouteDTO {
    private String type;
    private String id;
    private GeoJsonRouteGeometry geometry;
    private GeoJsonRouteProperties properties;
}
