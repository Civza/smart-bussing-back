package buss.smartbussingapi.DTOs.GeoJsonStops;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class GeoJsonStopGeometry {
        private String type;
        private List<Double> coordinates; // [[lon, lat], [lon, lat]]
}

