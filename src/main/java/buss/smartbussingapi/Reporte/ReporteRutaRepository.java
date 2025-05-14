package buss.smartbussingapi.Reporte;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ReporteRutaRepository extends JpaRepository<ReporteRuta, Integer> {

}
