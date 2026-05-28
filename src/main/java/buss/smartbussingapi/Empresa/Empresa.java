package buss.smartbussingapi.Empresa;

import buss.smartbussingapi.Lugar.Lugar;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
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
    @Column(name = "id_empresa")
    @JsonProperty("id_empresa")
    private int idEmpresa;

    private String name;
    private String pais;

    @Column(name = "metodo_pago")
    @JsonProperty("metodo_pago")
    private String metodoPago;

    private String email;

    @OneToMany(mappedBy = "empresa", cascade = CascadeType.ALL)
    @JsonIgnore
    private List<Lugar> lugares;

}
