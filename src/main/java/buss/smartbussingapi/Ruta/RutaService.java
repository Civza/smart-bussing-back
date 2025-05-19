package buss.smartbussingapi.Ruta;

import buss.smartbussingapi.Coordenadas.Coordenadas;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;

@Service
public class RutaService {

    @Autowired
    private final RutaRepository rutaRepository;

    public RutaService(RutaRepository rutaRepository) {
        this.rutaRepository = rutaRepository;
    }

    public Ruta getRutaById(int ruta_id) {
        return rutaRepository.findById(ruta_id).get();
    }

    public List<Ruta> getAllRutas() {
        return rutaRepository.findAll();
    }

    public List<Coordenadas> getCoordenadasRuta(int ruta_id){
        Ruta ruta = rutaRepository.findById(ruta_id).orElseThrow(() -> new IllegalArgumentException("No se encontro el ruta"));
        return ruta.getCoordenadas();
    }

    public void agregarRuta(Ruta ruta){
        rutaRepository.save(ruta);
    }

    public void agregarCoordenadas(int ruta_id, List<Coordenadas> coordenadas){
        Ruta ruta = rutaRepository.findById(ruta_id).orElseThrow(() -> new IllegalArgumentException("No se encontro el ruta"));
        ruta.getCoordenadas().addAll(coordenadas);
        rutaRepository.save(ruta);
    }



}
