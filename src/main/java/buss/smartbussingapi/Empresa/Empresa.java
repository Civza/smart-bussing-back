package buss.smartbussingapi.Empresa;

import buss.smartbussingapi.Lugar.Lugar;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table
public class Empresa {

    @Id
    @SequenceGenerator(name = "empresa_sequence", sequenceName = "empresa_sequence" , allocationSize = 1)
    @GeneratedValue(strategy = GenerationType.SEQUENCE,generator = "empresa_sequence")
    private int id_empresa;
    private String name;
    private String pais;
    private String metodo_pago;

    @OneToMany(mappedBy = "empresa", cascade = CascadeType.ALL)
    private List<Lugar> lugares;

}
