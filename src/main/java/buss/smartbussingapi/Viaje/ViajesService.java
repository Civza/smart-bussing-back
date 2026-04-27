package buss.smartbussingapi.Viaje;

import buss.smartbussingapi.commons.exceptions.NotFoundException;
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

    public Viaje getViajebyId(int id_viaje) {
        return viajeRepository.findById(id_viaje)
                .orElseThrow(() -> new NotFoundException("Viaje with ID " + id_viaje + " not found"));
    }

    public List<Viaje> getAllViajes(){
        return viajeRepository.findAll();
    }

    public void addViaje(Viaje viaje){
        //TODO - PENDING LOGIC FOR TRAVEL - COMING ON ISSUE 17
        viajeRepository.save(viaje);
    }


}
