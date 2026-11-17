package buss.smartbussingapi.security;

import buss.smartbussingapi.commons.ApiResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

/** 401 / 403 responses in the API's usual {@link ApiResponse} shape, plus the Wazuh audit trail. */
@Component
public class JsonSecurityErrorHandlers {

    private static final Logger log = LoggerFactory.getLogger(JsonSecurityErrorHandlers.class);

    /** ApiResponse is a plain record; a private mapper avoids sharing Spring's mutable one. */
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    public AuthenticationEntryPoint entryPoint() {
        return (request, response, exception) -> {
            response.setHeader(HttpHeaders.WWW_AUTHENTICATE, "Bearer");
            write(response, HttpServletResponse.SC_UNAUTHORIZED, new ApiResponse<>("Unauthorized", null,
                    "Token de autenticacion ausente, invalido o expirado."));
        };
    }

    public AccessDeniedHandler accessDeniedHandler() {
        return (request, response, exception) -> {
            audit(request);
            write(response, HttpServletResponse.SC_FORBIDDEN, new ApiResponse<>("Forbidden", null,
                    "Acceso restringido. Se requiere rol de Administrador o Supervisor."));
        };
    }

    /** Same event the previous RoleAuthorizationService emitted, so Wazuh rules keep matching. */
    private void audit(HttpServletRequest request) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String user = authentication == null || authentication.getName() == null ? "anonymous" : authentication.getName();
        MDC.put("event_type", "security_alert");
        MDC.put("threat_type", "unauthorized_admin_access");
        MDC.put("user_email", user);
        MDC.put("http_status", "403");
        log.warn("Alerta de seguridad: Intento de acceso administrativo no autorizado por '{}' a {} {}",
                user, request.getMethod(), request.getRequestURI());
    }

    private void write(HttpServletResponse response, int status, ApiResponse<Void> body) throws IOException {
        response.setStatus(status);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        OBJECT_MAPPER.writeValue(response.getWriter(), body);
    }
}
