package buss.smartbussingapi.DTOs.GeoJsonStops;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class GeoJsonStopProperties {
    @JsonProperty("feature_type")
    private String featureType;

    @JsonProperty("stop_id")
    private String stopId;

    @JsonProperty("stop_name")
    private String stopName;

    @JsonProperty("stop_description")
    private String stopDescription;

    @JsonProperty("routes_names")
    private List<String> routesNames;
}