package buss.smartbussingapi.DTOs.GeoJsonStops;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class GeoJsonStopGeometry {
        private String type;
        private List<Double> coordinates; // [[lon, lat], [lon, lat]]

        public GeoJsonStopGeometry() {}

        public GeoJsonStopGeometry(String type, List<Double> coordinates) {
            this.type = type;
            this.coordinates = coordinates == null ? null : new java.util.ArrayList<>(coordinates);
        }
}

