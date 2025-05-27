package buss.smartbussingapi.Reporte;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ReporteRutaRepository extends JpaRepository<ReporteRuta, Integer> {

    @Query("SELECT likeRoute FROM ReporteRuta")
    List<Integer> getAllLikes(List<ReporteRuta> rutas);

    @Query(value = "SELECT rr FROM ReporteRuta rr JOIN rr.ruta r WHERE r.nombre_ruta = :routeName")
    List<ReporteRuta> getReporteRutasByRouteName(@Param("routeName") String routeName);

}
