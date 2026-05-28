package buss.smartbussingapi.Usuario;

import buss.smartbussingapi.DTOs.UsuarioDTO;
import buss.smartbussingapi.commons.exceptions.AlreadyExistsException;
import buss.smartbussingapi.commons.exceptions.InvalidCredentialsException;
import buss.smartbussingapi.commons.exceptions.InvalidDataException;
import buss.smartbussingapi.commons.exceptions.NotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UsuarioServiceTest {

    @Mock
    private UsuarioRepository usuarioRepository;

    @InjectMocks
    private UsuarioService usuarioService;

    private Usuario user;

    @BeforeEach
    void setUp() {
        user = new Usuario();
        user.setId_usuario(1);
        user.setNombre("John Doe");
        user.setEmail("test@test.com");
        user.setPassword("secret");
        user.setProfilePhotoURL("http://photo.com");
    }

    @Test
    void getUsuarios() {
        when(usuarioRepository.findAll()).thenReturn(List.of(user));
        assertEquals(1, usuarioService.getUsuarios().size());
    }

    @Test
    void getUsuarioById_Success() {
        when(usuarioRepository.findById(1)).thenReturn(Optional.of(user));
        assertEquals(user, usuarioService.getUsuarioById(1));
    }

    @Test
    void getUsuarioById_NotFound() {
        when(usuarioRepository.findById(1)).thenReturn(Optional.empty());
        assertThrows(NotFoundException.class, () -> usuarioService.getUsuarioById(1));
    }

    @Test
    void getUsuarioByEmail_Success() {
        when(usuarioRepository.findByEmail("test@test.com")).thenReturn(Optional.of(user));
        UsuarioDTO dto = usuarioService.getUsuarioByEmail("test@test.com");
        assertEquals("John Doe", dto.getNombre());
        assertEquals("test@test.com", dto.getEmail());
        assertEquals("http://photo.com", dto.getUrlPhoto());
    }

    @Test
    void getUsuarioByEmail_NotFound() {
        when(usuarioRepository.findByEmail("test@test.com")).thenReturn(Optional.empty());
        assertThrows(NotFoundException.class, () -> usuarioService.getUsuarioByEmail("test@test.com"));
    }

    @Test
    void getUsuarioIdByEmail_Success() {
        when(usuarioRepository.findByEmail("test@test.com")).thenReturn(Optional.of(user));
        assertEquals(1, usuarioService.getUsuarioIdByEmail("test@test.com"));
    }

    @Test
    void getUsuarioIdByEmail_NotFound() {
        when(usuarioRepository.findByEmail("test@test.com")).thenReturn(Optional.empty());
        assertThrows(NotFoundException.class, () -> usuarioService.getUsuarioIdByEmail("test@test.com"));
    }

    @Test
    void verifyCredential_Success() {
        when(usuarioRepository.findByEmail("test@test.com")).thenReturn(Optional.of(user));
        assertTrue(usuarioService.verifyCredential("test@test.com", "secret"));
    }

    @Test
    void verifyCredential_WrongPassword() {
        when(usuarioRepository.findByEmail("test@test.com")).thenReturn(Optional.of(user));
        assertThrows(InvalidCredentialsException.class, () -> usuarioService.verifyCredential("test@test.com", "wrong"));
    }

    @Test
    void verifyCredential_NotFound() {
        when(usuarioRepository.findByEmail("test@test.com")).thenReturn(Optional.empty());
        assertThrows(InvalidCredentialsException.class, () -> usuarioService.verifyCredential("test@test.com", "secret"));
    }

    @Test
    void registerUsuario_Success() {
        when(usuarioRepository.findByEmail(anyString())).thenReturn(Optional.empty());
        usuarioService.registerUsuario(user);
        verify(usuarioRepository, times(1)).save(user);
    }

    @Test
    void registerUsuario_AlreadyExists() {
        when(usuarioRepository.findByEmail(anyString())).thenReturn(Optional.of(user));
        assertThrows(AlreadyExistsException.class, () -> usuarioService.registerUsuario(user));
    }

    @Test
    void registerUsuario_EmptyName() {
        user.setNombre("");
        assertThrows(InvalidDataException.class, () -> usuarioService.registerUsuario(user));
    }

    @Test
    void registerUsuario_EmptyEmail() {
        user.setEmail(null);
        assertThrows(InvalidDataException.class, () -> usuarioService.registerUsuario(user));
    }

    @Test
    void registerUsuario_EmptyPassword() {
        user.setPassword("  ");
        assertThrows(InvalidDataException.class, () -> usuarioService.registerUsuario(user));
    }

    @Test
    void editProfileName_Success() {
        when(usuarioRepository.findById(1)).thenReturn(Optional.of(user));
        usuarioService.editProfileName(1, "New Name");
        assertEquals("New Name", user.getNombre());
        verify(usuarioRepository, times(1)).save(user);
    }

    @Test
    void editProfileName_SameName_StillSaves() {
        when(usuarioRepository.findById(1)).thenReturn(Optional.of(user));
        usuarioService.editProfileName(1, "John Doe");
        assertEquals("John Doe", user.getNombre());
        verify(usuarioRepository, times(1)).save(user);
    }

    @Test
    void editProfileName_NotFound() {
        when(usuarioRepository.findById(1)).thenReturn(Optional.empty());
        assertThrows(NotFoundException.class, () -> usuarioService.editProfileName(1, "New Name"));
    }

    @Test
    void editProfileName_EmptyName() {
        assertThrows(InvalidDataException.class, () -> usuarioService.editProfileName(1, ""));
    }

    @Test
    void editPassword_Success() {
        when(usuarioRepository.findById(1)).thenReturn(Optional.of(user));
        usuarioService.editPassword(1, "newpass");
        assertEquals("newpass", user.getPassword());
        verify(usuarioRepository, times(1)).save(user);
    }

    @Test
    void editPassword_SamePassword() {
        when(usuarioRepository.findById(1)).thenReturn(Optional.of(user));
        usuarioService.editPassword(1, "secret");
        verify(usuarioRepository, times(1)).save(user);
    }

    @Test
    void editPassword_NotFound() {
        when(usuarioRepository.findById(1)).thenReturn(Optional.empty());
        assertThrows(NotFoundException.class, () -> usuarioService.editPassword(1, "newpass"));
    }

    @Test
    void editPassword_Empty() {
        assertThrows(InvalidDataException.class, () -> usuarioService.editPassword(1, null));
    }
}
