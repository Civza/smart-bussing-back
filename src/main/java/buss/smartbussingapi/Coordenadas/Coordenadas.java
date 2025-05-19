package buss.smartbussingapi.Coordenadas;

import buss.smartbussingapi.Ruta.Ruta;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table
public class Coordenadas {

    @Id
    @SequenceGenerator(name = "coordenada_sequence", sequenceName = "coordenada_sequence" , allocationSize = 1)
    @GeneratedValue(strategy = GenerationType.SEQUENCE,generator = "coordenada_sequence")
    private int id_coordenada;
    private String longitud;
    private String latitud;

    @ManyToMany(mappedBy = "coordenadas", cascade = CascadeType.ALL)
    private List<Ruta> rutaList = new ArrayList<>();



}
