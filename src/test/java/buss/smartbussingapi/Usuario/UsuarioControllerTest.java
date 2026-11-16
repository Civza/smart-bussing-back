package buss.smartbussingapi.Usuario;

import buss.smartbussingapi.DTOs.ChangePasswordRequest;
import buss.smartbussingapi.DTOs.LoginRequest;
import buss.smartbussingapi.DTOs.LoginResponse;
import buss.smartbussingapi.DTOs.RegisterRequest;
import buss.smartbussingapi.DTOs.UpdateNameRequest;
import buss.smartbussingapi.DTOs.UsuarioDTO;
import buss.smartbussingapi.commons.ApiResponse;
import buss.smartbussingapi.security.JwtTokenService;
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

    @Mock
    private RoleAuthorizationService roleAuthorizationService;

    @Mock
    private JwtTokenService jwtTokenService;

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
        ApiResponse<List<Usuario>> response = usuarioController.getUsuarios("admin@cetys.mx");
        verify(roleAuthorizationService).requireAdmin("admin@cetys.mx");
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
        RegisterRequest request = new RegisterRequest("John Doe", "test@test.com", "secret123", "http://photo.com");
        ApiResponse<Void> response = usuarioController.registerUser(request);
        assertNotNull(response);
        assertEquals("User created", response.info());
        assertNull(response.response());
        verify(usuarioService, times(1)).registerUsuario(argThat(u ->
                "John Doe".equals(u.getNombre())
                        && "test@test.com".equals(u.getEmail())
                        && "secret123".equals(u.getPassword())
                        && u.getRol() == Rol.USER));
    }

    @Test
    void login_Success() {
        when(usuarioService.verifyCredential("test@test.com", "secret")).thenReturn(true);
        when(usuarioService.getUsuarioIdByEmail("test@test.com")).thenReturn(1);
        when(usuarioService.getUsuarioById(1)).thenReturn(user);
        when(jwtTokenService.issueToken(user)).thenReturn("signed.jwt.token");
        when(jwtTokenService.getTtlSeconds()).thenReturn(3600L);

        ResponseEntity<?> responseEntity = usuarioController.login(new LoginRequest("test@test.com", "secret"));
        assertNotNull(responseEntity);
        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());

        ApiResponse<?> body = (ApiResponse<?>) responseEntity.getBody();
        assertNotNull(body);
        assertEquals("Login successful", body.info());
        LoginResponse login = (LoginResponse) body.response();
        assertEquals("signed.jwt.token", login.token());
        assertEquals("Bearer", login.tokenType());
        assertEquals(3600L, login.expiresIn());
        assertEquals("test@test.com", login.usuario().getEmail());
    }

    @Test
    void login_Failed_UnreachableBranchCoverage() {
        // Stub to return false so we hit the otherwise unreachable false branch in login method
        when(usuarioService.verifyCredential("test@test.com", "wrong")).thenReturn(false);

        ResponseEntity<?> responseEntity = usuarioController.login(new LoginRequest("test@test.com", "wrong"));
        assertNotNull(responseEntity);
        assertEquals(HttpStatus.UNAUTHORIZED, responseEntity.getStatusCode());

        ApiResponse<?> body = (ApiResponse<?>) responseEntity.getBody();
        assertNotNull(body);
        assertEquals("Unauthorized", body.info());
        assertEquals("Invalid credentials", body.error());
    }

    @Test
    void modifyUserName() {
        when(usuarioService.getUsuarioIdByEmail("test@test.com")).thenReturn(1);
        doNothing().when(usuarioService).editProfileName(1, "New Name");
        ApiResponse<Void> response = usuarioController.updateMyName("test@test.com", new UpdateNameRequest("New Name"));
        assertNotNull(response);
        assertEquals("Username updated", response.info());
        assertNull(response.response());
        verify(usuarioService, times(1)).editProfileName(1, "New Name");
    }

    @Test
    void modifyPassword() {
        when(usuarioService.getUsuarioIdByEmail("test@test.com")).thenReturn(1);
        doNothing().when(usuarioService).changePassword(1, "secret", "newsecret");
        ApiResponse<Void> response = usuarioController.changeMyPassword("test@test.com",
                new ChangePasswordRequest("secret", "newsecret"));
        assertNotNull(response);
        assertEquals("Password updated", response.info());
        assertNull(response.response());
        verify(usuarioService, times(1)).changePassword(1, "secret", "newsecret");
    }
}
