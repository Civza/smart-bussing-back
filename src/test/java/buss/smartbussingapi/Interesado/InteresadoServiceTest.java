package buss.smartbussingapi.Interesado;

import buss.smartbussingapi.commons.exceptions.AlreadyExistsException;
import buss.smartbussingapi.commons.exceptions.InvalidDataException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InteresadoServiceTest {

    @Mock
    private InteresadoRepository interesadoRepository;

    @InjectMocks
    private InteresadoService interesadoService;

    @Test
    void findAll_Success() {
        when(interesadoRepository.findAll()).thenReturn(List.of(new Interesado()));
        List<Interesado> result = interesadoService.findAll();
        assertEquals(1, result.size());
    }

    @Test
    void addNewInteresado_NullEmail_ThrowsException() {
        Interesado interesado = new Interesado();
        interesado.setEmail(null);
        
        assertThrows(InvalidDataException.class, () -> interesadoService.addNewInteresado(interesado));
    }

    @Test
    void addNewInteresado_BlankEmail_ThrowsException() {
        Interesado interesado = new Interesado();
        interesado.setEmail("   ");
        
        assertThrows(InvalidDataException.class, () -> interesadoService.addNewInteresado(interesado));
    }

    @Test
    void addNewInteresado_AlreadyExists_ThrowsException() {
        Interesado interesado = new Interesado();
        interesado.setEmail("test@test.com");
        
        when(interesadoRepository.findById("test@test.com")).thenReturn(Optional.of(interesado));
        
        assertThrows(AlreadyExistsException.class, () -> interesadoService.addNewInteresado(interesado));
    }

    @Test
    void addNewInteresado_Success() {
        Interesado interesado = new Interesado();
        interesado.setEmail("test@test.com");
        
        when(interesadoRepository.findById("test@test.com")).thenReturn(Optional.empty());
        
        interesadoService.addNewInteresado(interesado);
        
        verify(interesadoRepository, times(1)).save(interesado);
    }
}
