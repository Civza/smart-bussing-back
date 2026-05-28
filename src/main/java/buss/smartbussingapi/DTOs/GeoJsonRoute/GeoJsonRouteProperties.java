package buss.smartbussingapi.DTOs.GeoJsonRoute;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class GeoJsonRouteProperties {
    @JsonProperty("feature_type")
    private String featureType;

    @JsonProperty("route_id")
    private String routeId;

    @JsonProperty("route_short_name")
    private String routeShortName;

    @JsonProperty("route_long_name")
    private String routeLongName;

    @JsonProperty("route_color")
    private String routeColor;

    @JsonProperty("route_text_color")
    private String routeTextColor;

    @JsonProperty("route_type")
    private String routeType;
}