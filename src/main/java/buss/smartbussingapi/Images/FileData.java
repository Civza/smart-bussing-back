package buss.smartbussingapi.Images;

import buss.smartbussingapi.Lugar.Lugar;
import jakarta.persistence.*;
import lombok.*;


@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "FILE_DATA")
public class FileData {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id_image;

    private String fileName;
    private String path;
    private String type;

    @ManyToOne
    @JoinColumn(name = "image_id")
    private Lugar lugar;
}