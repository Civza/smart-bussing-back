package buss.smartbussingapi.DTOs;

import lombok.Getter;
import lombok.Setter;

import java.util.Date;

@Getter
@Setter
public class RegistroLugarDTO {

    //Para los datos de la empresa
    private String nombreEmpresa;
    private String paisEmpresa;
    private String metodoPago;

    //Para registrar el lugar
    private String nombreLugar;
    private String direccion;
    private String tipo;
    private String telefono;
    private String descripcion;

    //Para la generacion del contrato
    private Date fechaInicio;
    private Date fechaFin;
    private int monto;

}
