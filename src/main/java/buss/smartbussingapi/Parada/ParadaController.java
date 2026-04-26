package buss.smartbussingapi.Parada;

import buss.smartbussingapi.commons.ApiResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
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
    @ResponseStatus(HttpStatus.OK)
    public ApiResponse<List<Parada>> getAllParadas() {
        return new ApiResponse<>("All paradas retrieved", paradaService.getParadasList(), null);
    }

    @GetMapping("/{id_parada}")
    @ResponseStatus(HttpStatus.OK)
    public ApiResponse<Parada> getParadaById(@PathVariable("id_parada") int id_parada) {
        return new ApiResponse<>("Parada retrieved", paradaService.getParadaById(id_parada), null);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<Void> addParada(@RequestBody Parada parada) {
        paradaService.addParada(parada);
        return new ApiResponse<>("Parada created", null, null);
    }

}
