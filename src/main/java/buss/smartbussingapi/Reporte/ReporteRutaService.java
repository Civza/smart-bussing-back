package buss.smartbussingapi.Reporte;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.PriorityQueue;

@Service
public class ReporteRutaService {

    @Autowired
    private final ReporteRutaRepository reporteRutaRepository;

    public ReporteRutaService(ReporteRutaRepository reporteRutaRepository) {
        this.reporteRutaRepository = reporteRutaRepository;
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

    public void addReporteRuta(ReporteRuta reporteRuta) {
        reporteRutaRepository.save(reporteRuta);
    }

}
