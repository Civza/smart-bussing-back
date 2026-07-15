package buss.smartbussingapi.Usuario;

import buss.smartbussingapi.DTOs.UsuarioDTO;
import buss.smartbussingapi.commons.ApiResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UsuarioControllerTest {

    @Mock
    private UsuarioService usuarioService;

    @InjectMocks
    private UsuarioController usuarioController;

    private Usuario user;

    @BeforeEach
    void setUp() {
        user = new Usuario();
        user.setIdUsuario(1);
        user.setNombre("John Doe");
        user.setEmail("test@test.com");
        user.setPassword("secret");
        user.setProfilePhotoURL("http://photo.com");
    }

    @Test
    void getUsuarios() {
        when(usuarioService.getUsuarios()).thenReturn(List.of(user));
        ApiResponse<List<Usuario>> response = usuarioController.getUsuarios();
        assertNotNull(response);
        assertEquals("All users retrieved", response.info());
        assertEquals(1, response.response().size());
        assertEquals("John Doe", response.response().get(0).getNombre());
    }

    @Test
    void getUsuarioById() {
        when(usuarioService.getUsuarioById(1)).thenReturn(user);
        ApiResponse<Usuario> response = usuarioController.getUsuarioById(1);
        assertNotNull(response);
        assertEquals("User retrieved", response.info());
        assertEquals("John Doe", response.response().getNombre());
    }

    @Test
    void getUsuarioByEmail() {
        UsuarioDTO dto = new UsuarioDTO();
        dto.setNombre("John Doe");
        dto.setEmail("test@test.com");
        dto.setUrlPhoto("http://photo.com");

        when(usuarioService.getUsuarioByEmail("test@test.com")).thenReturn(dto);
        ApiResponse<UsuarioDTO> response = usuarioController.getUsuarioByEmail("test@test.com");
        assertNotNull(response);
        assertEquals("User retrieved", response.info());
        assertEquals("John Doe", response.response().getNombre());
        assertEquals("test@test.com", response.response().getEmail());
    }

    @Test
    void registerUser() {
        doNothing().when(usuarioService).registerUsuario(user);
        ApiResponse<Void> response = usuarioController.registerUser(user);
        assertNotNull(response);
        assertEquals("User created", response.info());
        assertNull(response.response());
        verify(usuarioService, times(1)).registerUsuario(user);
    }

    @Test
    void login_Success() {
        when(usuarioService.verifyCredential("test@test.com", "secret")).thenReturn(true);
        when(usuarioService.getUsuarioIdByEmail("test@test.com")).thenReturn(1);
        when(usuarioService.getUsuarioById(1)).thenReturn(user);

        ResponseEntity<?> responseEntity = usuarioController.login("test@test.com", "secret");
        assertNotNull(responseEntity);
        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());

        ApiResponse<?> body = (ApiResponse<?>) responseEntity.getBody();
        assertNotNull(body);
        assertEquals("Login successful", body.info());
        assertEquals(user, body.response());
    }

    @Test
    void login_Failed_UnreachableBranchCoverage() {
        // Stub to return false so we hit the otherwise unreachable false branch in login method
        when(usuarioService.verifyCredential("test@test.com", "wrong")).thenReturn(false);

        ResponseEntity<?> responseEntity = usuarioController.login("test@test.com", "wrong");
        assertNotNull(responseEntity);
        assertEquals(HttpStatus.UNAUTHORIZED, responseEntity.getStatusCode());

        ApiResponse<?> body = (ApiResponse<?>) responseEntity.getBody();
        assertNotNull(body);
        assertEquals("Unauthorized", body.info());
        assertEquals("Invalid credentials", body.error());
    }

    @Test
    void modifyUserName() {
        doNothing().when(usuarioService).editProfileName(1, "New Name");
        ApiResponse<Void> response = usuarioController.modifyUserName(1, "New Name");
        assertNotNull(response);
        assertEquals("Username updated", response.info());
        assertNull(response.response());
        verify(usuarioService, times(1)).editProfileName(1, "New Name");
    }

    @Test
    void modifyPassword() {
        doNothing().when(usuarioService).editPassword(1, "newsecret");
        ApiResponse<Void> response = usuarioController.modifyPassword(1, "newsecret");
        assertNotNull(response);
        assertEquals("Password updated", response.info());
        assertNull(response.response());
        verify(usuarioService, times(1)).editPassword(1, "newsecret");
    }
}
