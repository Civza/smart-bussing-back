package buss.smartbussingapi.DTOs.ItineraryDTOs;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class ItineraryResponseDTO {
    private List<SegmentoResponseDTO> segmentos;
    private double distanciaTotalMetros;
    private double duracionTotalSegundos;
}
