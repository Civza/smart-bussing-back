package buss.smartbussingapi.Viaje;

import buss.smartbussingapi.DTOs.DirectionsResponse;
import buss.smartbussingapi.DTOs.ItineraryDTOs.ItineraryResponseDTO;
import buss.smartbussingapi.Viaje.CreatingOptimistPath.MapboxService;
import buss.smartbussingapi.commons.ApiResponse;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("api/v1/viaje")
public class ViajeController {

    private final ViajesService viajesService;
    private final MapboxService mapboxService;

    public ViajeController(ViajesService viajesService, MapboxService mapboxService) {
        this.viajesService = viajesService;
        this.mapboxService = mapboxService;
    }

    @GetMapping("/{id_viaje}")
    @ResponseStatus(HttpStatus.OK)
    public ApiResponse<Viaje> getViaje(@PathVariable("id_viaje") int idViaje) {
        return new ApiResponse<>("Viaje retrieved", viajesService.getViajebyId(idViaje), null);
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

    @GetMapping("/draft/get-travel")
    @ResponseStatus(HttpStatus.OK)
    public ApiResponse<ItineraryResponseDTO> getDraftOfPosibleRoute(
            @RequestParam("userLat") double userLat,
            @RequestParam("userLon") double userLon,
            @RequestParam("destLat") double destLat,
            @RequestParam("destLon") double destLon) {
        var response = viajesService.getDraftRoute(userLat, userLon, destLat, destLon);
        return new ApiResponse<>("Draft created", response, null);
    }

    //Endpoint just for test ONLY -> Posibili addapt it to store it or delete it
    @GetMapping("/get-walking-to-stop")
    @ResponseStatus(HttpStatus.OK)
    public ApiResponse<DirectionsResponse> getStepsForNearestStop(
            @RequestParam("userLat") Double userLat,
            @RequestParam("userLon") Double userLon) {
        var parada = mapboxService.findNearestStop(userLat, userLon);
        return new ApiResponse<>("Path to walk get it",
                mapboxService.getWalkingDirections(userLat, userLon, parada), null);
    }
}
