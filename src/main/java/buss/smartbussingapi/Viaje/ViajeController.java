package buss.smartbussingapi.Viaje;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("api/v1/viaje")
public class ViajeController {

    @Autowired
    private final ViajesService viajesService;

    public ViajeController(ViajesService viajesService) {
        this.viajesService = viajesService;
    }

    @GetMapping("/{id_viaje}")
    public Viaje getViaje(@PathVariable("id_viaje") int id_viaje){
        return viajesService.getViajebyId(id_viaje);
    }

    @GetMapping
    public List<Viaje> getViajes(){
        return viajesService.getAllViajes();
    }

    @PostMapping
    public void addViaje(@RequestBody  Viaje viaje){
        viajesService.addViaje(viaje);
    }
}
