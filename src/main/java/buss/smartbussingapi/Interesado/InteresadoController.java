package buss.smartbussingapi.Interesado;

import buss.smartbussingapi.commons.ApiResponse;
import lombok.AllArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
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
    @ResponseStatus(HttpStatus.OK)
    public ApiResponse<List<Interesado>> getInteresados() {
        return new ApiResponse<>("All interesados retrieved", interesadoService.findAll(), null);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<Void> addNewInteresado(@RequestBody Interesado interesado) {
        interesadoService.addNewInteresado(interesado);
        return new ApiResponse<>("Interesado created", null, null);
    }
}
