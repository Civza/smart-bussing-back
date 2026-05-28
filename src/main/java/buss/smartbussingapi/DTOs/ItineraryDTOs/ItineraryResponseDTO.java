package buss.smartbussingapi.DTOs.ItineraryDTOs;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

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
