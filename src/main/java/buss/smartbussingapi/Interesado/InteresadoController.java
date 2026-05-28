package buss.smartbussingapi.Interesado;

import buss.smartbussingapi.commons.ApiResponse;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("api/v1/interesado")
public class InteresadoController {

    private final InteresadoService interesadoService;
    // EI_EXPOSE_REP2: Java bean that needs reference injection
    @SuppressWarnings("EI_EXPOSE_REP2")
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
