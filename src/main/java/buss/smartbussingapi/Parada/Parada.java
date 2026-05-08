package buss.smartbussingapi.Parada;

import buss.smartbussingapi.Coordenadas.Coordenadas;
import buss.smartbussingapi.Ruta.Ruta;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;
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
    private int id_parada;
    private String nombre_parada;
    private String descripcion_parada;

    @OneToOne(cascade = CascadeType.ALL)
    private Coordenadas coordenadas_parada;

    @ManyToMany(mappedBy = "paradas")
    @JsonIgnore
    private List<Ruta> rutas = new ArrayList<>();
}
