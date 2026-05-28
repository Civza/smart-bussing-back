package buss.smartbussingapi.Reporte;

import buss.smartbussingapi.DTOs.ReporteRutaDTO;
import buss.smartbussingapi.commons.ApiResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

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
    @ResponseStatus(HttpStatus.OK)
    public ApiResponse<List<ReporteRuta>> getReporteRuta() {
        return new ApiResponse<>("All route reports retrieved", reporteRutaService.getReportesRuta(), null);
    }

    @GetMapping("/{reporte_ruta_id}")
    @ResponseStatus(HttpStatus.OK)
    public ApiResponse<ReporteRuta> getReporteRutaById(
            @PathVariable("reporte_ruta_id") int reporteRutaId
    ) {
        return new ApiResponse<>(
                "Route report retrieved",
                reporteRutaService.getReporteRutaById(reporteRutaId),
                null
        );
    }

    @GetMapping("/byRuta")
    @ResponseStatus(HttpStatus.OK)
    public ApiResponse<List<ReporteRuta>> getReporteRutaByRouteName(@RequestParam String routeName) {
        return new ApiResponse<>(
                "Route reports retrieved",
                reporteRutaService.getReportesRutaByRouteName(routeName),
                null
        );
    }

    @PostMapping("/{id_ruta}")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<ReporteRuta> createNewReporteRuta(
            @PathVariable("id_ruta") Integer idRuta,
            @RequestBody ReporteRutaDTO reporteRutaDTO,
            @RequestParam("email") String email
    ) {
        return new ApiResponse<>(
                "Nuevo reporte en la ruta : " + idRuta + " fue creado exitosamente",
                reporteRutaService.createNewReporteRuta(reporteRutaDTO, idRuta, email),
                null
        );
    }

}
