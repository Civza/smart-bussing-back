package buss.smartbussingapi.Parada;

import buss.smartbussingapi.Ruta.Ruta;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ParadaRepository extends JpaRepository<Parada, Integer> {

    @Query("SELECT r FROM Ruta r JOIN r.paradas p WHERE p.id_parada = :idParada")
    List<Ruta> findRutasByParadaId(@Param("idParada") int idParada);
}
