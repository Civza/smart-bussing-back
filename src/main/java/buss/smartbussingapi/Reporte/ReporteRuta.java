package buss.smartbussingapi.Reporte;

import buss.smartbussingapi.Ruta.Ruta;
import buss.smartbussingapi.Usuario.Usuario;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table
public class ReporteRuta {

    @Id
    @SequenceGenerator(name = "reporte_sequence", sequenceName = "reporte_sequence" , allocationSize = 1)
    @GeneratedValue(strategy = GenerationType.SEQUENCE,generator = "reporte_sequence")
    private int id_reporteRuta;
    private String descripcion;
    private int likeRoute;

    @ManyToOne
    @JoinColumn(name = "ruta_id")
    private Ruta ruta;

    @ManyToOne
    @JoinColumn(name = "usuario_id")
    private Usuario usuario;

}
