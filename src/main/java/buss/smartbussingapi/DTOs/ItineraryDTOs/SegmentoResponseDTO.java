package buss.smartbussingapi.DTOs.ItineraryDTOs;

import buss.smartbussingapi.Parada.Parada;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class SegmentoResponseDTO {
    private String tipo;                  // "WALKING" o "BUS"
    private String descripcion;

    private double distanciaMetros;
    private double duracionSegundos;

    private String geoJson;

    // Solo BUS
    private Parada paradaAbordaje;
    private Parada paradaDescenso;
    private List<Parada> paradas;
    private String nombreRuta;
    private String colorRuta;

    // Solo WALKING
    private Parada paradaDestino;
}
