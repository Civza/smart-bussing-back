package buss.smartbussingapi;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class SecurityAuditFilter implements Filter {

    private static final Logger log = LoggerFactory.getLogger(SecurityAuditFilter.class);

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest req = (HttpServletRequest) request;
        HttpServletResponse res = (HttpServletResponse) response;

        try {
            // Se agregan campos estructurados que el encoder meterá en el JSON
            MDC.put("client_ip", req.getRemoteAddr());
            MDC.put("http_method", req.getMethod());
            MDC.put("request_uri", req.getRequestURI());
            MDC.put("query_params", req.getQueryString() != null ? req.getQueryString() : "");

            chain.doFilter(request, response);

            MDC.put("http_status", String.valueOf(res.getStatus()));

            // Si es un intento sospechoso o error de auth, registramos WARN
            if (res.getStatus() == 401 || res.getStatus() == 403) {
                log.warn("Acceso denegado o fallo de autenticacion");
            } else if (req.getRequestURI().contains("..") || (req.getQueryString() != null && req.getQueryString().contains(".."))) {
                log.error("Alerta de seguridad: Posible intento de Path Traversal detectado");
            } else {
                log.info("Acceso web procesado exitosamente");
            }
        } finally {
            MDC.clear();
        }
    }
}