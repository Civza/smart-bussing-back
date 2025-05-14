package buss.smartbussingapi.Ruta;

import buss.smartbussingapi.Coordenadas.Coordenadas;
import buss.smartbussingapi.Reporte.ReporteRuta;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table
public class Ruta {
    @Id
    @SequenceGenerator(name = "ruta_sequence", sequenceName = "ruta_sequence" , allocationSize = 1)
    @GeneratedValue(strategy = GenerationType.SEQUENCE,generator = "ruta_sequence")
    private int id_ruta;
    private String nombre_ruta;
    private String horario_ruta;
    private boolean ac1tive;

    @OneToMany(mappedBy = "ruta", cascade = CascadeType.ALL)
    @JsonIgnore
    private List<ReporteRuta> reporteRutas;

    @ManyToMany(cascade = CascadeType.ALL)
    @JoinTable(
            name = "coordenadas_ruta",
            joinColumns = @JoinColumn(name = "ruta_id"),
            inverseJoinColumns = @JoinColumn(name = "coordenadas_id")
    )
    private List<Coordenadas> coordenadas = new ArrayList<>();


}
