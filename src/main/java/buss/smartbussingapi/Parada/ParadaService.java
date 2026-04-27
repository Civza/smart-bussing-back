package buss.smartbussingapi.Parada;

import buss.smartbussingapi.commons.exceptions.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ParadaService {

    @Autowired
    private final ParadaRepository paradaRepository;

    public ParadaService(ParadaRepository paradaRepository) {
        this.paradaRepository = paradaRepository;
    }

    public List<Parada> getParadasList(){
        return paradaRepository.findAll();
    }

    public Parada getParadaById(int parada_id) {
        return paradaRepository.findById(parada_id)
                .orElseThrow(() -> new NotFoundException("Parada with ID " + parada_id + " not found"));
    }

    public void addParada(Parada parada){
        //TODO - PARADA LOGIC ON ISSUE 17
        paradaRepository.save(parada);
    }

}
