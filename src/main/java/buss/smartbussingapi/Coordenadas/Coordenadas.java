package buss.smartbussingapi.Coordenadas;

import buss.smartbussingapi.Ruta.Ruta;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToMany;
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
public class Coordenadas {

    @Id
    @SequenceGenerator(name = "coordenada_sequence", sequenceName = "coordenada_sequence" , allocationSize = 1)
    @GeneratedValue(strategy = GenerationType.SEQUENCE,generator = "coordenada_sequence")
    @Column(name = "id_coordenada")
    @JsonProperty("id_coordenada")
    private int idCoordenada;
    private Double longitud;
    private Double latitud;
    private String sentido; // e.g., "IDA", "REGRESO", "AMBOS"

    @ManyToMany(mappedBy = "coordenadas", cascade = CascadeType.ALL)
    @JsonIgnore
    private List<Ruta> rutaList = new ArrayList<>();


    public Coordenadas(double v, double v1, String ida) {
        this.latitud = v;
        this.longitud = v1;
        this.sentido = ida;
    }
}
