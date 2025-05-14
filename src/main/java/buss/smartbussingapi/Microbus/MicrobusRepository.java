package buss.smartbussingapi.Microbus;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface MicrobusRepository extends JpaRepository<Microbus, Integer> {

}
