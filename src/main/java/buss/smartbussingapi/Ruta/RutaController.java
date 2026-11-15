package buss.smartbussingapi.Ruta;

import buss.smartbussingapi.Coordenadas.Coordenadas;
import buss.smartbussingapi.DTOs.GeoJsonFeatureCollectionDTO;
import buss.smartbussingapi.Usuario.RoleAuthorizationService;
import buss.smartbussingapi.commons.ApiResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/ruta")
public class RutaController {

    private final RutaService rutaService;
    private final RoleAuthorizationService roleAuthorizationService;

    @Autowired
    public RutaController(RutaService rutaService, RoleAuthorizationService roleAuthorizationService) {
        this.rutaService = rutaService;
        this.roleAuthorizationService = roleAuthorizationService;
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
    public ApiResponse<Ruta> addNewRuta(
            @RequestHeader(value = "X-User-Email", required = false) String userEmail,
            @RequestBody GeoJsonFeatureCollectionDTO payload) {
        roleAuthorizationService.requireAdmin(userEmail);
        return new ApiResponse<>("Route created", rutaService.agregarRutaDesdeGeoJson(payload), null);
    }

    /*
    @PostMapping("/{id_ruta}/coor")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<Void> addCoordenadas(@PathVariable("id_ruta") int id_ruta, @RequestBody List<Coordenadas> coordenadas) {
        rutaService.agregarCoordenadas(id_ruta, coordenadas);
        return new ApiResponse<>("Coordinates added", null, null);
    }

     */



}
