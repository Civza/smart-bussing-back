package buss.smartbussingapi.DTOs;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.List;

public record GeoJsonFeatureCollectionDTO(String type, List<JsonNode> features) {

    public GeoJsonFeatureCollectionDTO {
        features = List.copyOf(features);
    }

}
