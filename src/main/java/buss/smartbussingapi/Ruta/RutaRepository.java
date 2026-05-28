package buss.smartbussingapi.Ruta;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RutaRepository extends JpaRepository<Ruta, Integer> {

    @Query("SELECT r FROM Ruta r WHERE r.nombreRuta = :nombreRuta")
    Ruta findRutaByNombreRuta(@Param("nombreRuta") String nombreRuta);
    @Query("""
            SELECT DISTINCT r
            FROM Ruta r
            LEFT JOIN FETCH r.coordenadas
            WHERE r.active = true
            """)
    List<Ruta> findAllActiveWithCoordenadas();
}
