package buss.smartbussingapi.Lugar;

import buss.smartbussingapi.commons.ApiResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("api/v1/lugar")
public class LugarController {

    @Autowired
    private final LugarService lugarService;

    public LugarController(LugarService lugarService) {
        this.lugarService = lugarService;
    }

    @GetMapping
    @ResponseStatus(HttpStatus.OK)
    public ApiResponse<List<Lugar>> getLugares() {
        return new ApiResponse<>("All lugares retrieved", lugarService.findAlllugares(), null);
    }

    @GetMapping(path = "/{id_lugar}")
    @ResponseStatus(HttpStatus.OK)
    public ApiResponse<Lugar> getLugar(@PathVariable("id_lugar") int id_lugar) {
        return new ApiResponse<>("Lugar retrieved", lugarService.findLugarById(id_lugar), null);
    }


}
