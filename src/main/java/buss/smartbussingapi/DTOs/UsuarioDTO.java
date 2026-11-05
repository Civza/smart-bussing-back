package buss.smartbussingapi.DTOs;

import buss.smartbussingapi.Usuario.Rol;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UsuarioDTO {
    private String nombre;
    private String email;
    private String urlPhoto;
    private Rol rol;
}
