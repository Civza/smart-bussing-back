package buss.smartbussingapi.Ruta;

import buss.smartbussingapi.Coordenadas.Coordenadas;
import buss.smartbussingapi.commons.ApiResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/ruta")
public class RutaController {

    @Autowired
    private final RutaService rutaService;

    public RutaController(RutaService rutaService) {
        this.rutaService = rutaService;
    }

    @GetMapping
    @ResponseStatus(HttpStatus.OK)
    public ApiResponse<List<Ruta>> getRutas() {
        return new ApiResponse<>("All routes retrieved", rutaService.getAllRutas(), null);
    }

    @GetMapping("/{id_ruta}")
    @ResponseStatus(HttpStatus.OK)
    public ApiResponse<Ruta> getRuta(@PathVariable("id_ruta") int id_ruta) {
        return new ApiResponse<>("Route retrieved", rutaService.getRutaById(id_ruta), null);
    }

    @GetMapping("/coordenadas/{id_ruta}")
    @ResponseStatus(HttpStatus.OK)
    public ApiResponse<List<Coordenadas>> getCoordenadasByRuta(@PathVariable("id_ruta") int id_ruta) {
        return new ApiResponse<>("Coordinates retrieved", rutaService.getCoordenadasRuta(id_ruta), null);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<Void> addNewRuta(@RequestBody Ruta ruta) {
        rutaService.agregarRuta(ruta);
        return new ApiResponse<>("Route created", null, null);
    }

    @PostMapping("/{id_ruta}/coor")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<Void> addCoordenadas(@PathVariable("id_ruta") int id_ruta, @RequestBody List<Coordenadas> coordenadas) {
        rutaService.agregarCoordenadas(id_ruta, coordenadas);
        return new ApiResponse<>("Coordinates added", null, null);
    }



}
