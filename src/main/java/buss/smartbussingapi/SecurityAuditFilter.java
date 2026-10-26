package buss.smartbussingapi;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.regex.Pattern;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class SecurityAuditFilter implements Filter {

    private static final Logger log = LoggerFactory.getLogger(SecurityAuditFilter.class);

    // Patrones regex para deteccion proactiva de ataques en URI o query params
    private static final Pattern SQL_INJECTION_PATTERN = Pattern.compile(
            "(?i)(\\b(union\\s+select|select\\s+.*\\s+from|insert\\s+into|delete\\s+from|drop\\s+table|drop\\s+database|alter\\s+table)\\b|'\\s*(or|and)\\s*('|[\\d=])|--|/\\*|\\*/|;\\s*(drop|delete|insert))"
    );

    private static final Pattern PATH_TRAVERSAL_PATTERN = Pattern.compile(
            "(\\.\\./|\\.\\.\\\\|%2e%2e%2f|%2e%2e/|%2e%2e%5c)"
    );

    private static final Pattern XSS_PATTERN = Pattern.compile(
            "(?i)(<script|javascript:|onerror\\s*=|onload\\s*=|<iframe)"
    );

    private final RateLimiterService rateLimiterService;

    @SuppressWarnings("EI_EXPOSE_REP2")
    public SecurityAuditFilter(RateLimiterService rateLimiterService) {
        this.rateLimiterService = rateLimiterService;
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest req = (HttpServletRequest) request;
        HttpServletResponse res = (HttpServletResponse) response;

        String clientIp = resolveClientIp(req);
        String requestUri = req.getRequestURI();
        String queryString = req.getQueryString() != null ? req.getQueryString() : "";

        try {
            // Contexto MDC inicial para Logstash / Wazuh
            MDC.put("client_ip", clientIp);
            MDC.put("http_method", req.getMethod());
            MDC.put("request_uri", requestUri);
            MDC.put("query_params", queryString);

            // Inyectar cabeceras de seguridad HTTP recomendadas (Politica 2 y OWASP Top 10)
            injectSecurityHeaders(res);

            // 1. Deteccion e inspeccion temprana de amenazas (SQLi, Path Traversal, XSS)
            String threatDetected = inspectRequestForThreats(requestUri, queryString);
            if (threatDetected != null) {
                MDC.put("event_type", "security_alert");
                MDC.put("threat_type", threatDetected);
                MDC.put("http_status", "403");

                log.error("Alerta de seguridad: Intento de ataque detectado [{}] desde IP {} en URI {}",
                        threatDetected, clientIp, requestUri);

                sendSecurityErrorResponse(res, HttpServletResponse.SC_FORBIDDEN,
                        "Acceso bloqueado por politicas de seguridad: " + threatDetected);
                return;
            }

            // 2. Control de Tasa de Peticiones (Rate Limiting)
            if (!rateLimiterService.isAllowed(clientIp, requestUri)) {
                MDC.put("event_type", "rate_limit_exceeded");
                MDC.put("threat_type", "RATE_LIMIT_EXCEEDED");
                MDC.put("http_status", "429");

                res.setHeader("Retry-After", String.valueOf(rateLimiterService.getRetryAfterSeconds()));

                log.warn("Alerta de seguridad: Tasa de solicitudes excedida (Rate Limit) para la IP {} en {}",
                        clientIp, requestUri);

                sendSecurityErrorResponse(res, 429,
                        "Has excedido el limite permitido de solicitudes. Por favor intente mas tarde.");
                return;
            }

            // Ejecutar la cadena de procesamiento de Spring
            chain.doFilter(request, response);

            // Registro posterior al procesamiento segun el codigo HTTP de respuesta
            int status = res.getStatus();
            MDC.put("http_status", String.valueOf(status));

            if (status == 401) {
                MDC.put("event_type", "auth_failure");
                log.warn("Fallo de autenticacion: Credenciales invalidas o no proporcionadas");
            } else if (status == 403) {
                MDC.put("event_type", "resource_forbidden");
                log.warn("Acceso denegado: Recurso bloqueado o permisos insuficientes");
            } else if (status == 429) {
                MDC.put("event_type", "rate_limit_exceeded");
                log.warn("Alerta de seguridad: Limite de peticiones excedido (HTTP 429)");
            } else if (status >= 500) {
                MDC.put("event_type", "server_error");
                log.error("Error interno del servidor procesando solicitud");
            } else if (status == 404) {
                MDC.put("event_type", "resource_not_found");
                log.info("Recurso no encontrado (HTTP 404)");
            } else {
                MDC.put("event_type", "access_success");
                log.info("Acceso web procesado exitosamente");
            }

        } finally {
            MDC.clear();
        }
    }

    private void injectSecurityHeaders(HttpServletResponse res) {
        res.setHeader("X-Content-Type-Options", "nosniff");
        res.setHeader("X-Frame-Options", "DENY");
        res.setHeader("X-XSS-Protection", "1; mode=block");
        res.setHeader("Strict-Transport-Security", "max-age=31536000; includeSubDomains");
        res.setHeader("Referrer-Policy", "strict-origin-when-cross-origin");
    }

    private String inspectRequestForThreats(String uri, String query) {
        String decodedQuery = query;
        try {
            if (query != null && !query.isBlank()) {
                decodedQuery = URLDecoder.decode(query, StandardCharsets.UTF_8);
            }
        } catch (Exception ignored) {
            // En caso de que falle la decodificacion, mantener la query sin decodificar
        }

        String fullTarget = uri + "?" + decodedQuery;

        if (PATH_TRAVERSAL_PATTERN.matcher(fullTarget).find()) {
            return "PATH_TRAVERSAL";
        }
        if (SQL_INJECTION_PATTERN.matcher(fullTarget).find()) {
            return "SQL_INJECTION";
        }
        if (XSS_PATTERN.matcher(fullTarget).find()) {
            return "XSS_ATTACK";
        }

        return null;
    }

    private void sendSecurityErrorResponse(HttpServletResponse res, int statusCode, String message)
            throws IOException {
        res.setStatus(statusCode);
        res.setContentType("application/json;charset=UTF-8");
        String json = String.format(
                "{\"info\":\"Security Block\",\"response\":null,\"error\":\"%s\"}",
                escapeJson(message)
        );
        res.getWriter().write(json);
        res.getWriter().flush();
    }

    private String escapeJson(String input) {
        if (input == null) return "";
        return input.replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "\\r");
    }

    private String resolveClientIp(HttpServletRequest req) {
        String forwarded = req.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        String realIp = req.getHeader("X-Real-IP");
        if (realIp != null && !realIp.isBlank()) {
            return realIp.trim();
        }
        return req.getRemoteAddr() != null ? req.getRemoteAddr() : "127.0.0.1";
    }
}
