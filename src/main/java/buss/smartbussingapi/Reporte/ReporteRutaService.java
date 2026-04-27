package buss.smartbussingapi.Reporte;

import buss.smartbussingapi.DTOs.ReporteRutaDTO;
import buss.smartbussingapi.Ruta.Ruta;
import buss.smartbussingapi.Ruta.RutaRepository;
import buss.smartbussingapi.Usuario.Usuario;
import buss.smartbussingapi.Usuario.UsuarioRepository;
import buss.smartbussingapi.commons.exceptions.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

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

    public List<ReporteRuta> getReportesRuta() {
        List<ReporteRuta> rr = reporteRutaRepository.findAll();
        List<Integer> likes = reporteRutaRepository.getAllLikes(rr);
        return rr;
    }

    public List<ReporteRuta> getReportesRutaByRouteName(String routeName) {
        List<ReporteRuta> reportes = reporteRutaRepository.getReporteRutasByRouteName(routeName);
        if (reportes.isEmpty()) {
            throw new NotFoundException("No reports found for route: " + routeName);
        }
        return reportes;
    }

    public ReporteRuta getReporteRutaById(int id_reporteRuta) {
        return reporteRutaRepository.findById(id_reporteRuta)
                .orElseThrow(() -> new NotFoundException("Report with ID " + id_reporteRuta + " not found"));
    }

    public ReporteRuta createNewReporteRuta(ReporteRutaDTO reporteRutaDTO, Integer id_ruta, String email) {
        Ruta ruta = rutaRepository.findById(id_ruta)
                .orElseThrow(() -> new NotFoundException("Route with ID " + id_ruta + " not found"));

        Usuario user = usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new NotFoundException("User with email " + email + " not found"));

        ReporteRuta newReporteRuta = new ReporteRuta();
        newReporteRuta.setDescripcion(reporteRutaDTO.getDescripcion());
        newReporteRuta.setUrlPhotos(reporteRutaDTO.getUrlPhoto());
        newReporteRuta.setRuta(ruta);
        newReporteRuta.setUsuario(user);
        newReporteRuta.setLikeRoute(0);

        return reporteRutaRepository.save(newReporteRuta);
    }
}