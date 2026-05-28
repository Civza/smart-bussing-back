package buss.smartbussingapi.DTOs.ItineraryDTOs;

import buss.smartbussingapi.DTOs.DirectionsResponse;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class SegmentoResponseDTO {
    private String tipo;                  // "WALKING" o "BUS"
    private String descripcion;
    private DirectionsResponse directions;

}
