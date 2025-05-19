package buss.smartbussingapi.Microbus;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class MicrobusService {
    @Autowired
    private final MicrobusRepository microbusRepository;

    public MicrobusService(MicrobusRepository microbusRepository) {
        this.microbusRepository = microbusRepository;
    }

    public List<Microbus> getAllMicrobuses() {
        return microbusRepository.findAll();
    }

    public void addMicrobus(Microbus microbus) {
        microbusRepository.save(microbus);
    }
}
