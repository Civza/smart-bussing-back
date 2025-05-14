package buss.smartbussingapi.Parada;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("api/v1/parada")
public class ParadaController {

    @Autowired
    private final ParadaService paradaService;

    public ParadaController(ParadaService paradaService) {
        this.paradaService = paradaService;
    }

    @GetMapping
    public List<Parada> getAllParadas(){
        return paradaService.getParadasList();
    }

    @GetMapping("/{id_parada}")
    public Parada getParadaById(@PathVariable("id_parada") int id_parada){
        return paradaService.getParadaById(id_parada);
    }

    @PostMapping
    public void addParada(@RequestBody Parada parada){
        paradaService.addParada(parada);
    }

}
