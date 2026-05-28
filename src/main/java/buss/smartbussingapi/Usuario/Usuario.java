package buss.smartbussingapi.Usuario;

import buss.smartbussingapi.Reporte.ReporteRuta;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;

import java.util.List;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Accessors(fluent = false)
@Entity
@Table
public class Usuario {
    @Id
    @SequenceGenerator(name = "usuario_sequence", sequenceName = "usuario_sequence" , allocationSize = 1)
    @GeneratedValue(strategy = GenerationType.SEQUENCE,generator = "usuario_sequence")
    @Column(name = "id_usuario")
    @JsonProperty("id_usuario")
    private int idUsuario;
    private String nombre;
    private String email;
    private String password;
    private String profilePhotoURL;

    @OneToMany(mappedBy = "usuario", cascade = CascadeType.ALL)
    @JsonIgnore
    private List<ReporteRuta> reporteRutas;

}


