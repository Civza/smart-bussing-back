package buss.smartbussingapi.Usuario;

import buss.smartbussingapi.commons.exceptions.AccessDeniedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class RoleAuthorizationService {

    private static final Logger log = LoggerFactory.getLogger(RoleAuthorizationService.class);

    private final UsuarioRepository usuarioRepository;

    @Autowired
    public RoleAuthorizationService(UsuarioRepository usuarioRepository) {
        this.usuarioRepository = usuarioRepository;
    }

    /**
     * Valida que el correo proporcionado corresponda a un usuario existente con rol ADMIN.
     * Si no cumple, audita la alerta en MDC para Wazuh y lanza AccessDeniedException (HTTP 403).
     *
     * @param userEmail Correo del usuario que intenta realizar la accion administrativa
     */
    public void requireAdmin(String userEmail) {
        if (userEmail == null || userEmail.trim().isEmpty()) {
            auditUnauthorizedAttempt("anonymous", "Email no proporcionado en peticion administrativa");
            throw new AccessDeniedException("Se requiere identificacion de usuario supervisor para realizar esta accion.");
        }

        String sanitizedEmail = userEmail.trim();
        Usuario usuario = usuarioRepository.findByEmail(sanitizedEmail)
                .orElse(null);

        if (usuario == null) {
            auditUnauthorizedAttempt(sanitizedEmail, "Usuario no encontrado en la base de datos");
            throw new AccessDeniedException("Usuario no autorizado para realizar acciones administrativas.");
        }

        if (usuario.getRol() != Rol.ADMIN) {
            auditUnauthorizedAttempt(sanitizedEmail, "El usuario posee rol " + usuario.getRol() + ", requiere ADMIN");
            throw new AccessDeniedException("Acceso restringido. Se requiere rol de Administrador o Supervisor.");
        }
    }

    private void auditUnauthorizedAttempt(String userEmail, String reason) {
        MDC.put("event_type", "security_alert");
        MDC.put("threat_type", "unauthorized_admin_access");
        MDC.put("user_email", userEmail);
        MDC.put("http_status", "403");
        log.warn("Alerta de seguridad: Intento de acceso administrativo no autorizado por '{}'. Motivo: {}", userEmail, reason);
    }
}
