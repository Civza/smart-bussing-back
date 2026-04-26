package buss.smartbussingapi.Reporte;

import buss.smartbussingapi.DTOs.ReporteRutaDTO;
import buss.smartbussingapi.commons.ApiResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
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

    @PostMapping("/{id_ruta}")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<ReporteRuta> createNewReporteRuta(@PathVariable("id_ruta") Integer id_ruta,@RequestBody ReporteRutaDTO reporteRutaDTO, @RequestParam("email") String email){
        return new ApiResponse<>("Nuevo reporte en la ruta : " + id_ruta + "fue creado exitosamente", reporteRutaService.createNewReporteRuta(reporteRutaDTO,id_ruta,email),null);
    }

}
