package buss.smartbussingapi.DTOs;

import buss.smartbussingapi.Usuario.Rol;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UsuarioDTO {
    @JsonProperty("id_usuario")
    private int idUsuario;
    private String nombre;
    private String email;
    private String urlPhoto;
    private Rol rol;
}
