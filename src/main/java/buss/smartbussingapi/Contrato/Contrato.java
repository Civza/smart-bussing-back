package buss.smartbussingapi.Contrato;

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
import lombok.experimental.Accessors;

import java.util.Date;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Accessors(fluent = false)
@Entity
@Table
public class Contrato {

    @Id
    @SequenceGenerator(name = "contrato_sequence", sequenceName = "contrato_sequence" , allocationSize = 1)
    @GeneratedValue(strategy = GenerationType.SEQUENCE,generator = "contrato_sequence")
    @Column(name = "id_contrato")
    @JsonProperty("id_contrato")
    private int idContrato;

    @Column(name = "fecha_inicio")
    @JsonProperty("fecha_inicio")
    private Date fechaInicio;

    @Column(name = "fecha_fin")
    @JsonProperty("fecha_fin")
    private Date fechaFin;

    private int monto;

    @OneToMany(mappedBy = "contrato", cascade = CascadeType.ALL)
    @JsonIgnore
    private List<Lugar> lugares;

}
