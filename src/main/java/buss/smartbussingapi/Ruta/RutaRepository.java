package buss.smartbussingapi.Ruta;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RutaRepository extends JpaRepository<Ruta, Integer> {

    @Query("SELECT r FROM Ruta r WHERE r.nombre_ruta = :nombre_ruta")
    Ruta findRutaByNombre_ruta(@Param("nombre_ruta") String nombre_ruta);
}
