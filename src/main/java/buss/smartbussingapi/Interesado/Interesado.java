package buss.smartbussingapi.Interesado;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table
public class Interesado {

    @Id
    @SequenceGenerator(name = "interesado_sequence", sequenceName = "interesado_sequence" , allocationSize = 1)
    @GeneratedValue(strategy = GenerationType.SEQUENCE,generator = "interesado_sequence")
    private String email;
}
