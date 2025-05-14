package buss.smartbussingapi.Ruta;

import buss.smartbussingapi.Coordenadas.Coordenadas;
import org.springframework.beans.factory.annotation.Autowired;
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
    public List<Ruta> getRutas() {
        return rutaService.getAllRutas();
    }

    @GetMapping("/{id_ruta}")
    public Ruta getRuta(@PathVariable("id_ruta") int id_ruta) {
        return rutaService.getRutaById(id_ruta);
    }

    @GetMapping("/coordenadas/{id_ruta}")
    public List<Coordenadas> getCoordenadasByRuta(@PathVariable("id_ruta") int id_ruta) {
        return rutaService.getCoordenadasRuta(id_ruta);
    }

    @PostMapping
    public void addNewRuta(@RequestBody Ruta ruta) {
        rutaService.agregarRuta(ruta);
    }

    @PostMapping("/{id_ruta}/coor")
    public void addCoordenadas(@PathVariable("id_ruta") int id_ruta,@RequestBody List<Coordenadas> coordenadas) {
        rutaService.agregarCoordenadas(id_ruta, coordenadas);
    }


}
