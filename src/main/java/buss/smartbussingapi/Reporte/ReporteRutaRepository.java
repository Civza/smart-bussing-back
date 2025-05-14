package buss.smartbussingapi.Reporte;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ReporteRutaRepository extends JpaRepository<ReporteRuta, Integer> {

    @Query("SELECT likeRoute FROM ReporteRuta")
    List<Integer> getAllLikes(List<ReporteRuta> rutas);

}
