package buss.smartbussingapi.Lugar;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class LugarService {

    @Autowired
    private final LugarRepository lugarRepository;

    public LugarService(LugarRepository lugarRepository) {
        this.lugarRepository = lugarRepository;
    }

    public List<Lugar> findAlllugares() {
        return lugarRepository.findAll();
    }

    public Lugar findLugarById(int id_lugar){
        return lugarRepository.findById(id_lugar).get();
    }
}
