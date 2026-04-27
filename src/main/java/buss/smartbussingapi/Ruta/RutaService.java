package buss.smartbussingapi.Ruta;

import buss.smartbussingapi.Coordenadas.Coordenadas;
import buss.smartbussingapi.commons.exceptions.InvalidDataException;
import buss.smartbussingapi.commons.exceptions.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class RutaService {

    @Autowired
    private final RutaRepository rutaRepository;

    public RutaService(RutaRepository rutaRepository) {
        this.rutaRepository = rutaRepository;
    }

    public Ruta getRutaById(int ruta_id) {
        return rutaRepository.findById(ruta_id)
                .orElseThrow(() -> new NotFoundException("Route with ID " + ruta_id + " not found"));
    }

    public List<Ruta> getAllRutas() {
        return rutaRepository.findAll();
    }

    public List<Coordenadas> getCoordenadasRuta(int ruta_id) {
        Ruta ruta = rutaRepository.findById(ruta_id)
                .orElseThrow(() -> new NotFoundException("Route with ID " + ruta_id + " not found"));
        if (ruta.getCoordenadas().isEmpty()) {
            throw new NotFoundException("No coordinates found for route with ID " + ruta_id);
        }
        return ruta.getCoordenadas();
    }

    public void agregarRuta(Ruta ruta) {
        //TODO - NEW LOGIC COMING SOON
        rutaRepository.save(ruta);
    }

    public void agregarCoordenadas(int ruta_id, List<Coordenadas> coordenadas) {
        if (coordenadas == null || coordenadas.isEmpty()) {
            throw new InvalidDataException("Coordinates list cannot be empty");
        }
        Ruta ruta = rutaRepository.findById(ruta_id)
                .orElseThrow(() -> new NotFoundException("Route with ID " + ruta_id + " not found"));

        ruta.getCoordenadas().addAll(coordenadas);
        rutaRepository.save(ruta);
    }
}