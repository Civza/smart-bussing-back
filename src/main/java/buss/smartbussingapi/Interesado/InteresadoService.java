package buss.smartbussingapi.Interesado;

import buss.smartbussingapi.commons.exceptions.AlreadyExistsException;
import buss.smartbussingapi.commons.exceptions.InvalidDataException;
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
        if (interesado.getEmail() == null || interesado.getEmail().isBlank()) {
            throw new InvalidDataException("Email cannot be empty");
        }

        if (interesadoRepository.findById(interesado.getEmail()).isPresent()) {
            throw new AlreadyExistsException("Interesado with email " + interesado.getEmail() + " already exists");
        }

        interesadoRepository.save(interesado);
    }
}
