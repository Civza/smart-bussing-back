package buss.smartbussingapi.Parada;

import buss.smartbussingapi.Coordenadas.Coordenadas;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table
public class Parada {

    @Id
    @SequenceGenerator(name = "parada_sequence", sequenceName = "parada_sequence" , allocationSize = 1)
    @GeneratedValue(strategy = GenerationType.SEQUENCE,generator = "parada_sequence")
    private int id_parada;
    private String nombre_parada;
    private String zona_parada;
    private int tiempo_Espera;

    @OneToOne
    private Coordenadas coordenadas_parada;
}
