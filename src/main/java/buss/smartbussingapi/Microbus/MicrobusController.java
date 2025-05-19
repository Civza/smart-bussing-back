package buss.smartbussingapi.Microbus;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("api/v1/microbus")
public class MicrobusController {

    @Autowired
    private final MicrobusService microbusService;

    public MicrobusController(MicrobusService microbusService){
        this.microbusService = microbusService;
    }

    @GetMapping
    private List<Microbus> getMicrobus(){
       return microbusService.getAllMicrobuses();
    }

    @PostMapping
    private void addMicrobus(@RequestBody Microbus microbus){
        microbusService.addMicrobus(microbus);
    }
}
