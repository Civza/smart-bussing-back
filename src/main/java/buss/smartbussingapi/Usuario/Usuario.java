package buss.smartbussingapi.Usuario;

import jakarta.persistence.*;
import lombok.*;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Entity
@Table
public class Usuario {
    @Id
    @SequenceGenerator(name = "usuario_sequence", sequenceName = "usuario_sequence" , allocationSize = 1)
    @GeneratedValue(strategy = GenerationType.SEQUENCE,generator = "usuario_sequence")
    private int id_usuario;
    private String nombre;
    private String email;
    private String password;


}


