package buss.smartbussingapi.Lugar;

import buss.smartbussingapi.Contrato.Contrato;
import buss.smartbussingapi.Empresa.Empresa;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
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
@NoArgsConstructor
@AllArgsConstructor
@Accessors(fluent = false)
@Entity
@Table
public class Lugar {

    @Id
    @SequenceGenerator(name = "lugar_sequence", sequenceName = "lugar_sequence" , allocationSize = 1)
    @GeneratedValue(strategy = GenerationType.SEQUENCE,generator = "lugar_sequence")
    @Column(name = "lugar_id")
    @JsonProperty("lugar_id")
    private int lugarId;
    private String name;
    private String tipo;
    private String telefono;
    private String descripcion;
    private String direccion;
    private List<String> urlFiles;

    @ManyToOne()
    @JoinColumn(name = "lugar_empresa_id")
    private Empresa empresa;

    @ManyToOne
    @JoinColumn(name = "lugar_contrato_id")
    private Contrato contrato;

}
