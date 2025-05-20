package buss.smartbussingapi.Interesado;

import lombok.AllArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("api/v1/interesado")
public class InteresadoController {

    @Autowired
    private final InteresadoService interesadoService;

    public InteresadoController(InteresadoService interesadoService) {
        this.interesadoService = interesadoService;
    }

    @GetMapping
    private List<Interesado> getInteresados() {
        return interesadoService.findAll();
    }

    @PostMapping
    private void addNewInteresado(@RequestBody Interesado interesado) {
        interesadoService.addNewInteresado(interesado);
    }
}
