package buss.smartbussingapi.Viaje;

import buss.smartbussingapi.Coordenadas.Coordenadas;
import buss.smartbussingapi.DTOs.DirectionsResponse;
import buss.smartbussingapi.Parada.Parada;
import buss.smartbussingapi.Viaje.CreatingOptimistPath.MapboxService;
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
    private final MapboxService mapboxService;

    public ViajeController(ViajesService viajesService, MapboxService mapboxService) {
        this.viajesService = viajesService;
        this.mapboxService = mapboxService;
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
        /*
        viajesService.addViaje(viaje);

         */
        return new ApiResponse<>("Viaje created", null, null);
    }

    //Endpoint just for test ONLY -> Posibili addapt it to store it or delete it
    @GetMapping("/get-walking-to-stop")
    @ResponseStatus(HttpStatus.OK)
    public ApiResponse<DirectionsResponse> getStepsForNearestStop(@RequestParam("userLat") Double userLat, @RequestParam("userLon") Double userLon){
        var parada = mapboxService.findNearestStop(userLat,userLon);
        return new ApiResponse<>("Path to walk get it", mapboxService.getWalkingDirections(userLat,userLon,parada), null);
    }
}
