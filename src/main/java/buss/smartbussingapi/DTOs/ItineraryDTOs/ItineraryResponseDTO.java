package buss.smartbussingapi.DTOs.ItineraryDTOs;

import lombok.Data;

import java.util.List;

@Data
public class ItineraryResponseDTO {
    private List<SegmentoResponseDTO> segmentos;
    private double distanciaTotalMetros;
    private double duracionTotalSegundos;
}
