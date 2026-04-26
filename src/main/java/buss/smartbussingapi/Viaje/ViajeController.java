package buss.smartbussingapi.Viaje;

import buss.smartbussingapi.commons.ApiResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
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
    @ResponseStatus(HttpStatus.OK)
    public ApiResponse<Viaje> getViaje(@PathVariable("id_viaje") int id_viaje) {
        return new ApiResponse<>("Viaje retrieved", viajesService.getViajebyId(id_viaje), null);
    }

    @GetMapping
    @ResponseStatus(HttpStatus.OK)
    public ApiResponse<List<Viaje>> getViajes() {
        return new ApiResponse<>("All viajes retrieved", viajesService.getAllViajes(), null);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<Void> addViaje(@RequestBody Viaje viaje) {
        viajesService.addViaje(viaje);
        return new ApiResponse<>("Viaje created", null, null);
    }
}
