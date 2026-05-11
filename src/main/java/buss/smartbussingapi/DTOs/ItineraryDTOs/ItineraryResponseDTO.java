package buss.smartbussingapi.DTOs.ItineraryDTOs;

import lombok.*;

import java.util.List;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ItineraryResponseDTO {
    private List<SegmentoResponseDTO> segmentos;
    private double distanciaTotalMetros;
    private double duracionTotalSegundos;
}
