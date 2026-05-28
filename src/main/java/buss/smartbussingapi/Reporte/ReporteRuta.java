package buss.smartbussingapi.Reporte;

import buss.smartbussingapi.Ruta.Ruta;
import buss.smartbussingapi.Usuario.Usuario;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Accessors(fluent = false)
@Entity
@Table
public class ReporteRuta {

    @Id
    @SequenceGenerator(name = "reporte_sequence", sequenceName = "reporte_sequence" , allocationSize = 1)
    @GeneratedValue(strategy = GenerationType.SEQUENCE,generator = "reporte_sequence")
    @Column(name = "id_reporte_ruta")
    @JsonProperty("id_reporte_ruta")
    private int idReporteRuta;
    private String descripcion;
    private int likeRoute;
    private String[] urlPhotos;

    @ManyToOne
    @JoinColumn(name = "ruta_id")
    private Ruta ruta;

    @ManyToOne
    @JoinColumn(name = "usuario_id")
    private Usuario usuario;

}
