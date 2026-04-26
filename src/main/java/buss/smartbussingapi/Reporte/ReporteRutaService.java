package buss.smartbussingapi.Reporte;

import buss.smartbussingapi.DTOs.ReporteRutaDTO;
import buss.smartbussingapi.Ruta.Ruta;
import buss.smartbussingapi.Ruta.RutaRepository;
import buss.smartbussingapi.Usuario.Usuario;
import buss.smartbussingapi.Usuario.UsuarioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.PriorityQueue;

@Service
public class ReporteRutaService {

    @Autowired
    private final ReporteRutaRepository reporteRutaRepository;
    private final RutaRepository rutaRepository;
    private final UsuarioRepository usuarioRepository;

    public ReporteRutaService(ReporteRutaRepository reporteRutaRepository, RutaRepository rutaRepository, UsuarioRepository usuarioRepository) {
        this.reporteRutaRepository = reporteRutaRepository;
        this.rutaRepository = rutaRepository;
        this.usuarioRepository = usuarioRepository;
    }

    public List<ReporteRuta> getReportesRuta(){
        List<ReporteRuta> rr = reporteRutaRepository.findAll();
        List<Integer> likes = reporteRutaRepository.getAllLikes(rr);

        return rr;
    }

    public List<ReporteRuta> getReportesRutaByRouteName(String routeName){
        return reporteRutaRepository.getReporteRutasByRouteName(routeName);
    }

    public ReporteRuta getReporteRutaById(int id_reporteRuta) {
        return reporteRutaRepository.findById(id_reporteRuta).get();
    }

    public ReporteRuta createNewReporteRuta(ReporteRutaDTO reporteRutaDTO, Integer id_ruta, String email){
        Optional<Ruta> optionalRuta = rutaRepository.findById(id_ruta);
        if(!optionalRuta.isPresent()){
            throw new IllegalArgumentException("The route with ID : " + id_ruta + " doesnt exist ");
        }

        Optional<Usuario> optionalUser = usuarioRepository.findByEmail(email);
        if(!optionalRuta.isPresent()){
            throw new IllegalArgumentException("The user with Email : " + email + " doesnt exist ");
        }

        Usuario user = optionalUser.get();

        Ruta ruta = optionalRuta.get();

        ReporteRuta newReporteRuta = new ReporteRuta();
        newReporteRuta.setDescripcion(reporteRutaDTO.getDescripcion());
        newReporteRuta.setUrlPhotos(reporteRutaDTO.getUrlPhoto());
        newReporteRuta.setRuta(ruta);
        newReporteRuta.setUsuario(user);
        newReporteRuta.setLikeRoute(0);

        return reporteRutaRepository.save(newReporteRuta);
    }

}
