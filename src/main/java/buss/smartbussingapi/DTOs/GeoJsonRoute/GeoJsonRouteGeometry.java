package buss.smartbussingapi.DTOs.GeoJsonRoute;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class GeoJsonRouteGeometry {
        private String type;
        private List<List<Double>> coordinates; // [[lon, lat], [lon, lat]]
}

