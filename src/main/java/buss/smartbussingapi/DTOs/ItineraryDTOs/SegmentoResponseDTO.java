package buss.smartbussingapi.DTOs.ItineraryDTOs;

import buss.smartbussingapi.DTOs.DirectionsResponse;
import buss.smartbussingapi.Parada.Parada;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@Builder
public class SegmentoResponseDTO {
    private String tipo;                  // "WALKING" o "BUS"
    private String descripcion;
    private DirectionsResponse directions;

}
