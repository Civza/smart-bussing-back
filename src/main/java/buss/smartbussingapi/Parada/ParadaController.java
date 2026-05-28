package buss.smartbussingapi.Parada;

import buss.smartbussingapi.DTOs.GeoJsonStops.GeoJsonStopDTO;
import buss.smartbussingapi.commons.ApiResponse;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("api/v1/parada")
public class ParadaController {


    private final ParadaService paradaService;

    // This is a Java bean that needs to be injected with the reference to the object
    @SuppressWarnings("EI_EXPOSE_REP2")
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
    public ApiResponse<Parada> getParadaById(@PathVariable("id_parada") int idParada) {
        return new ApiResponse<>("Parada retrieved", paradaService.getParadaById(idParada), null);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<Parada> createNewParada(@RequestBody GeoJsonStopDTO geoJsonStopDTO) {
        var parada = paradaService.addParada(geoJsonStopDTO);
        return new ApiResponse<>("Parada created", parada, null);
    }

}
