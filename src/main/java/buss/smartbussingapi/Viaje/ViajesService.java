package buss.smartbussingapi.Viaje;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class ViajesService {

    @Autowired
    private final ViajeRepository viajeRepository;

    public ViajesService(ViajeRepository viajeRepository) {
        this.viajeRepository = viajeRepository;
    }

    public Viaje getViajebyId(int id_viaje){
        Optional<Viaje> viaje = viajeRepository.findById(id_viaje);

        if(!viaje.isPresent()){
            throw new IllegalArgumentException("Viaje not found");
        }

        return viaje.get();
    }

    public List<Viaje> getAllViajes(){
        return viajeRepository.findAll();
    }

    public void addViaje(Viaje viaje){
        viajeRepository.save(viaje);
    }


}
