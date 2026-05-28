package buss.smartbussingapi.Ruta;

import buss.smartbussingapi.Coordenadas.Coordenadas;
import buss.smartbussingapi.Parada.Parada;
import buss.smartbussingapi.Reporte.ReporteRuta;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.OneToMany;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Accessors(fluent = false)
@Entity
@Table
public class Ruta {
    @Id
    @SequenceGenerator(name = "ruta_sequence", sequenceName = "ruta_sequence" , allocationSize = 1)
    @GeneratedValue(strategy = GenerationType.SEQUENCE,generator = "ruta_sequence")
    @Column(name = "id_ruta")
    @JsonProperty("id_ruta")
    private Integer idRuta;

    @Column(name = "nombre_ruta")
    @JsonProperty("nombre_ruta")
    private String nombreRuta;

    @Column(name = "nombre_corto_ruta")
    @JsonProperty("nombre_corto_ruta")
    private String nombreCortoRuta;

    @Column(name = "color_ruta")
    @JsonProperty("color_ruta")
    private String colorRuta;

    @Column(name = "color_texto_ruta")
    @JsonProperty("color_texto_ruta")
    private String colorTextoRuta;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_ruta")
    @JsonProperty("tipo_ruta")
    private RutaType tipoRuta;

    @Column(name = "horario_ruta")
    @JsonProperty("horario_ruta")
    private String horarioRuta;

    private boolean active;

    @Column(nullable = false, columnDefinition = "boolean default true")
    private boolean bidirectional = true;

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

    @ManyToMany(cascade = CascadeType.ALL)
    @JoinTable(
            name = "ruta_parada",
            joinColumns = @JoinColumn(name = "id_ruta"),
            inverseJoinColumns = @JoinColumn(name = "id_parada")
    )
    private List<Parada> paradas = new ArrayList<>();
}
