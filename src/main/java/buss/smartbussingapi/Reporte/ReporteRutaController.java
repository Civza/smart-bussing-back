package buss.smartbussingapi.Reporte;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/reporteRuta")
public class ReporteRutaController {

    @Autowired
    private final ReporteRutaService reporteRutaService;

    public ReporteRutaController(ReporteRutaService reporteRutaService) {
        this.reporteRutaService = reporteRutaService;
    }

    @GetMapping
    public List<ReporteRuta> getReporteRuta() {
        return reporteRutaService.getReportesRuta();
    }

    @GetMapping("/{reporte_ruta_id}")
    public ReporteRuta getReporteRutaById(@PathVariable("reporte_ruta_id") int reporte_ruta_id) {
        return reporteRutaService.getReporteRutaById(reporte_ruta_id);
    }

    @GetMapping("/byRuta")
    public List<ReporteRuta> getReporteRutaByRouteName(@RequestParam String routeName) {
        return reporteRutaService.getReportesRutaByRouteName(routeName);
    }

    @PostMapping
    public void addReporteRuta(@RequestBody ReporteRuta reporteRuta) {
        reporteRutaService.addReporteRuta(reporteRuta);
    }


}
