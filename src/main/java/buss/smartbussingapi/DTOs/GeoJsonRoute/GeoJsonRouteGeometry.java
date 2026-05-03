package buss.smartbussingapi.DTOs.GeoJsonRoute;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class GeoJsonRouteGeometry {
        private String type;
        private List<List<Double>> coordinates; // [[lon, lat], [lon, lat]]
}

