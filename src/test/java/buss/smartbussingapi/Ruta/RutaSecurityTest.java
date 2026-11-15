package buss.smartbussingapi.Ruta;

import buss.smartbussingapi.commons.exceptions.ResourceBlockedException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.context.ActiveProfiles;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ActiveProfiles("test")
@ExtendWith(MockitoExtension.class)
public class RutaSecurityTest {

    @Mock
    private RutaRepository rutaRepository;

    @Mock
    private buss.smartbussingapi.Viaje.CreatingOptimistPath.GraphBuilderService graphBuilderService;

    @InjectMocks
    private RutaService rutaService;

    @Test
    void getRutaById_ThrowsForbiddenWhenRouteIsInactive() {
        Ruta ruta = new Ruta();
        ruta.setIdRuta(10);
        ruta.setActive(false);

        when(rutaRepository.findById(10)).thenReturn(Optional.of(ruta));

        assertThrows(ResourceBlockedException.class, () -> rutaService.getRutaById(10));
    }

    @Test
    void getCoordenadasRuta_ThrowsForbiddenWhenRouteIsInactive() {
        Ruta ruta = new Ruta();
        ruta.setIdRuta(10);
        ruta.setActive(false);

        when(rutaRepository.findById(10)).thenReturn(Optional.of(ruta));

        assertThrows(ResourceBlockedException.class, () -> rutaService.getCoordenadasRuta(10));
    }
}
