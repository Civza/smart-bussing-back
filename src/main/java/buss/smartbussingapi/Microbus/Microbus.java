package buss.smartbussingapi.Microbus;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table
public class Microbus {

    @Id
    @SequenceGenerator(name = "microbus_sequence", sequenceName = "microbus_sequence" , allocationSize = 1)
    @GeneratedValue(strategy = GenerationType.SEQUENCE,generator = "microbus_sequence")
    private int id_bus;
    private String placa;
    private String modelo;
    private int capacidad;
    private String estado;


}
