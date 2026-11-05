package buss.smartbussingapi.Usuario;

import buss.smartbussingapi.commons.exceptions.AccessDeniedException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.MDC;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RoleAuthorizationServiceTest {

    @Mock
    private UsuarioRepository usuarioRepository;

    @InjectMocks
    private RoleAuthorizationService roleAuthorizationService;

    private Usuario adminUser;
    private Usuario regularUser;

    @BeforeEach
    void setUp() {
        MDC.clear();

        adminUser = new Usuario();
        adminUser.setIdUsuario(1);
        adminUser.setEmail("supervisor@cetys.mx");
        adminUser.setNombre("Supervisor General");
        adminUser.setRol(Rol.ADMIN);

        regularUser = new Usuario();
        regularUser.setIdUsuario(2);
        regularUser.setEmail("pasajero@cetys.mx");
        regularUser.setNombre("Pasajero Común");
        regularUser.setRol(Rol.USER);
    }

    @Test
    void requireAdmin_WhenUserIsAdmin_ShouldSucceed() {
        when(usuarioRepository.findByEmail("supervisor@cetys.mx")).thenReturn(Optional.of(adminUser));

        assertDoesNotThrow(() -> roleAuthorizationService.requireAdmin("supervisor@cetys.mx"));
    }

    @Test
    void requireAdmin_WhenUserIsRegularUser_ShouldThrowAccessDeniedExceptionAndAuditMdc() {
        when(usuarioRepository.findByEmail("pasajero@cetys.mx")).thenReturn(Optional.of(regularUser));

        AccessDeniedException ex = assertThrows(
                AccessDeniedException.class,
                () -> roleAuthorizationService.requireAdmin("pasajero@cetys.mx")
        );

        assertTrue(ex.getMessage().contains("Acceso restringido"));
        assertEquals("security_alert", MDC.get("event_type"));
        assertEquals("unauthorized_admin_access", MDC.get("threat_type"));
        assertEquals("pasajero@cetys.mx", MDC.get("user_email"));
        assertEquals("403", MDC.get("http_status"));
    }

    @Test
    void requireAdmin_WhenUserNotFound_ShouldThrowAccessDeniedExceptionAndAuditMdc() {
        when(usuarioRepository.findByEmail("inexistente@cetys.mx")).thenReturn(Optional.empty());

        AccessDeniedException ex = assertThrows(
                AccessDeniedException.class,
                () -> roleAuthorizationService.requireAdmin("inexistente@cetys.mx")
        );

        assertTrue(ex.getMessage().contains("Usuario no autorizado"));
        assertEquals("security_alert", MDC.get("event_type"));
        assertEquals("unauthorized_admin_access", MDC.get("threat_type"));
        assertEquals("inexistente@cetys.mx", MDC.get("user_email"));
        assertEquals("403", MDC.get("http_status"));
    }

    @Test
    void requireAdmin_WhenEmailIsNull_ShouldThrowAccessDeniedExceptionAndAuditMdc() {
        AccessDeniedException ex = assertThrows(
                AccessDeniedException.class,
                () -> roleAuthorizationService.requireAdmin(null)
        );

        assertTrue(ex.getMessage().contains("Se requiere identificacion"));
        assertEquals("security_alert", MDC.get("event_type"));
        assertEquals("unauthorized_admin_access", MDC.get("threat_type"));
        assertEquals("anonymous", MDC.get("user_email"));
        assertEquals("403", MDC.get("http_status"));
    }

    @Test
    void requireAdmin_WhenEmailIsBlank_ShouldThrowAccessDeniedExceptionAndAuditMdc() {
        AccessDeniedException ex = assertThrows(
                AccessDeniedException.class,
                () -> roleAuthorizationService.requireAdmin("   ")
        );

        assertTrue(ex.getMessage().contains("Se requiere identificacion"));
        assertEquals("security_alert", MDC.get("event_type"));
        assertEquals("unauthorized_admin_access", MDC.get("threat_type"));
        assertEquals("anonymous", MDC.get("user_email"));
        assertEquals("403", MDC.get("http_status"));
    }
}
