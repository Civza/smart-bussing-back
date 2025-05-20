package buss.smartbussingapi.Interesado;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class InteresadoService {
    @Autowired
    private final InteresadoRepository interesadoRepository;

    public InteresadoService(InteresadoRepository interesadoRepository) {
        this.interesadoRepository = interesadoRepository;
    }

    public List<Interesado> findAll() {
        return interesadoRepository.findAll();
    }

    public void addNewInteresado(Interesado interesado) {

        Optional<Interesado> findByEmail = interesadoRepository.findById(interesado.getEmail());

        if (findByEmail.isPresent()) {
            throw new IllegalArgumentException("Este interesado ya existe");
        }

        if(interesado.getEmail().isEmpty()){
            throw new IllegalArgumentException("Hay un campo vacio");
        }

        interesadoRepository.save(interesado);
    }
}
