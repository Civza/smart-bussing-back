package buss.smartbussingapi.Viaje;

import buss.smartbussingapi.Microbus.Microbus;
import buss.smartbussingapi.Parada.Parada;
import buss.smartbussingapi.Ruta.Ruta;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table
public class Viaje {

    @Id
    @SequenceGenerator(name = "viaje_sequence", sequenceName = "viaje_sequence" , allocationSize = 1)
    @GeneratedValue(strategy = GenerationType.SEQUENCE,generator = "viaje_sequence")
    private int id_viaje;
    private int tiempo_viaje;
    private double costo_viaje;

    @ManyToMany
    @JoinTable(
            name = "viaje_ruta",
            joinColumns = @JoinColumn(name = "id_viaje"),
            inverseJoinColumns = @JoinColumn( name = "id_ruta")
    )
    private List<Ruta> rutas_viaje;

    @ManyToMany
    @JoinTable(
            name = "viaje_parada",
            joinColumns = @JoinColumn(name = "id_viaje"),
            inverseJoinColumns = @JoinColumn(name = "id_parada")
    )
    private List<Parada> paradas_viaje;

    @ManyToMany
    @JoinTable(
            name = "viaje_microbus",
            joinColumns = @JoinColumn(name = "id_viaje"),
            inverseJoinColumns = @JoinColumn(name = "id_microbus")
    )
    private List<Microbus> microbus_viaje;
}
