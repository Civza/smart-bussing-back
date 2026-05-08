package buss.smartbussingapi.DTOs;

import lombok.Getter;
import lombok.Setter;

import java.util.Date;
import java.util.List;

@Getter
@Setter
public class RegistroLugarDTO {

    //Para los datos de la empresa
    private String nombreEmpresa;
    //private String paisEmpresa;
    private String correo_empresa;

    //Para registrar el lugar
    private String nombreLugar;
    private String tipo;
    private String telefono;
    private String descripcion;
    private String direccion;

    private List<String> urlFiles;

    //Para la generacion del contrato
    //private Date fechaInicio;
   // private Date fechaFin;
   // private int monto;

}
