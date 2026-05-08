package buss.smartbussingapi.DTOs.GeoJsonRoute;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class GeoJsonRouteProperties {
        private String feature_type;
        private String route_id;
        private String route_short_name;
        private String route_long_name;
        private String route_color;
        private String route_text_color;
        private String route_type;
}