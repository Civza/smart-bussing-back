package buss.smartbussingapi.Viaje;

import buss.smartbussingapi.Parada.Parada;
import buss.smartbussingapi.Ruta.Ruta;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;

import java.util.List;

@Getter
@Setter
@Accessors(fluent = false)
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table
public class Viaje {

    @Id
    @SequenceGenerator(name = "viaje_sequence", sequenceName = "viaje_sequence", allocationSize = 1)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "viaje_sequence")
    @Column(name = "id_viaje")
    @JsonProperty("id_viaje")
    private int idViaje;

    @Column(name = "tiempo_viaje")
    @JsonProperty("tiempo_viaje")
    private int tiempoViaje;

    @Column(name = "costo_viaje")
    @JsonProperty("costo_viaje")
    private double costoViaje;

    @ManyToMany
    @JoinTable(
            name = "viaje_ruta",
            joinColumns = @JoinColumn(name = "id_viaje"),
            inverseJoinColumns = @JoinColumn(name = "id_ruta")
    )
    @JsonProperty("rutas_viaje")
    private List<Ruta> rutasViaje;

    @ManyToMany
    @JoinTable(
            name = "viaje_parada",
            joinColumns = @JoinColumn(name = "id_viaje"),
            inverseJoinColumns = @JoinColumn(name = "id_parada")
    )
    @JsonProperty("paradas_viaje")
    private List<Parada> paradasViaje;

}
