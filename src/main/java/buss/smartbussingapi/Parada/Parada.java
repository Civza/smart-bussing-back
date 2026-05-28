package buss.smartbussingapi.Parada;

import buss.smartbussingapi.Coordenadas.Coordenadas;
import buss.smartbussingapi.Ruta.Ruta;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.OneToOne;
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
public class Parada {

    @Id
    @SequenceGenerator(name = "parada_sequence", sequenceName = "parada_sequence" , allocationSize = 1)
    @GeneratedValue(strategy = GenerationType.SEQUENCE,generator = "parada_sequence")
    @Column(name = "id_parada")
    @JsonProperty("id_parada")
    private int idParada;

    @Column(name = "nombre_parada")
    @JsonProperty("nombre_parada")
    private String nombreParada;

    @Column(name = "descripcion_parada")
    @JsonProperty("descripcion_parada")
    private String descripcionParada;

    @OneToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "coordenadas_parada_id_coordenada")
    @JsonProperty("coordenadas_parada")
    private Coordenadas coordenadasParada;

    @ManyToMany(mappedBy = "paradas")
    @JsonIgnore
    private List<Ruta> rutas = new ArrayList<>();
}
