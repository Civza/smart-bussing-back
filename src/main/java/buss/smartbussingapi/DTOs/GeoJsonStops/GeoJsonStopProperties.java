package buss.smartbussingapi.DTOs.GeoJsonStops;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class GeoJsonStopProperties {
        private String feature_type;
        private String stop_id;
        private String stop_name;
        private String stop_description;
        private List<String> routes_names;
}