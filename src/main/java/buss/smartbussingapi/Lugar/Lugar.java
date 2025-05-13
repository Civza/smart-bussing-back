package buss.smartbussingapi.Lugar;

import buss.smartbussingapi.Contrato.Contrato;
import buss.smartbussingapi.Empresa.Empresa;
import buss.smartbussingapi.Images.FileData;
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
public class Lugar {

    @Id
    @SequenceGenerator(name = "lugar_sequence", sequenceName = "lugar_sequence" , allocationSize = 1)
    @GeneratedValue(strategy = GenerationType.SEQUENCE,generator = "lugar_sequence")
    private int lugar_id;
    private String name;
    private String direccion;
    private String tipo;
    private String telefono;
    private String descripcion;

    @ManyToOne()
    @JoinColumn(name = "lugar_empresa_id")
    private Empresa empresa;

    @OneToMany(mappedBy = "lugar", cascade = CascadeType.ALL)
    private List<FileData> images;

    @ManyToOne
    @JoinColumn(name = "lugar_contrato_id")
    private Contrato contrato;

}
