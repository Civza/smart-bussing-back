package buss.smartbussingapi.Lugar;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
    public List<Lugar> getLugares() {
        return lugarService.findAlllugares();
    }

    @GetMapping(path = "/{id_lugar}")
    public Lugar getLugar(@PathVariable("id_lugar") int id_lugar) {
        return lugarService.findLugarById(id_lugar);
    }


}
