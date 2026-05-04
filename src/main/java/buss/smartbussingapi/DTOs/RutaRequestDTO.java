package buss.smartbussingapi.DTOs;

public record RutaRequestDTO(String nombre_ruta,String nombre_corto_ruta,String color_ruta,String color_texto_ruta, String tipo_ruta, String horario_ruta, boolean active) {
}
