package buss.smartbussingapi.Contrato;

import buss.smartbussingapi.Lugar.Lugar;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Date;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table
public class Contrato {

    @Id
    @SequenceGenerator(name = "contrato_sequence", sequenceName = "contrato_sequence" , allocationSize = 1)
    @GeneratedValue(strategy = GenerationType.SEQUENCE,generator = "contrato_sequence")
    private int id_contrato;
    private Date fecha_inicio;
    private Date fecha_fin;
    private int monto;

    @OneToMany(mappedBy = "contrato", cascade = CascadeType.ALL)
    private List<Lugar> lugares;

}
