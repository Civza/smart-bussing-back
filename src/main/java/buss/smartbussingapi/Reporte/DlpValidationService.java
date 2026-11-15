package buss.smartbussingapi.Reporte;

import buss.smartbussingapi.commons.exceptions.DlpViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

@Service
public class DlpValidationService {

    private static final Logger log = LoggerFactory.getLogger(DlpValidationService.class);

    private final RestTemplate restTemplate;

    @Value("${dlp.service.url:http://localhost:8000/api/dlp/scan}")
    private String dlpServiceUrl = "http://localhost:8000/api/dlp/scan";

    // Reglas regex de respaldo local (en caso de que el sidecar Python esté apagado)
    private static final Pattern LOCAL_PII_PHONE_PATTERN = Pattern.compile(
            "\\b(\\+?\\d{1,3}[- .]?)?\\(?\\d{3}\\)?[- .]?\\d{3}[- .]?\\d{4}\\b"
    );

    private static final List<String> LOCAL_BANNED_WORDS = List.of(
            "groseria", "insulto", "ofensivo", "estupido", "idiota", "incompetente", "corrupto", "maldito", "basura"
    );

    // Constructor por defecto
    public DlpValidationService() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(1200);
        factory.setReadTimeout(2000);
        this.restTemplate = new RestTemplate(factory);
    }

    // Constructor principal para inyección de Spring
    @Autowired
    public DlpValidationService(
            @Autowired(required = false) RestTemplateBuilder restTemplateBuilder,
            @Value("${dlp.service.url:http://localhost:8000/api/dlp/scan}") String dlpServiceUrl
    ) {
        if (restTemplateBuilder != null) {
            this.restTemplate = restTemplateBuilder
                    .connectTimeout(Duration.ofMillis(1200))
                    .readTimeout(Duration.ofMillis(2000))
                    .build();
        } else {
            SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
            factory.setConnectTimeout(1200);
            factory.setReadTimeout(2000);
            this.restTemplate = new RestTemplate(factory);
        }
        this.dlpServiceUrl = dlpServiceUrl;
    }

    // Constructor para inyección en pruebas unitarias
    public DlpValidationService(RestTemplate restTemplate, String dlpServiceUrl) {
        this.restTemplate = restTemplate;
        this.dlpServiceUrl = dlpServiceUrl;
    }

    public void validateComment(String text, Integer rutaId, String email) {
        if (text == null || text.isBlank()) {
            return;
        }

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            Map<String, Object> body = Map.of(
                    "text", text,
                    "user_email", email != null ? email : "",
                    "route_id", rutaId != null ? rutaId : 0
            );

            HttpEntity<Map<String, Object>> requestEntity = new HttpEntity<>(body, headers);
            Map response = restTemplate.postForObject(dlpServiceUrl, requestEntity, Map.class);

            if (response != null && Boolean.FALSE.equals(response.get("is_safe"))) {
                String violationType = (String) response.get("violation_type");
                if (violationType == null || violationType.isBlank()) {
                    violationType = "OBSCENE_CONTENT";
                }

                recordSecurityAlert(violationType, rutaId, email);
                throw new DlpViolationException("El comentario fue rechazado debido a que contiene lenguaje inapropiado o datos confidenciales (" + violationType + ")");
            }

        } catch (DlpViolationException e) {
            throw e;
        } catch (Exception ex) {
            log.warn("Microservicio DataFog DLP no disponible en {}. Aplicando filtro de respaldo local: {}", dlpServiceUrl, ex.getMessage());
            fallbackLocalCheck(text, rutaId, email);
        }
    }

    private void fallbackLocalCheck(String text, Integer rutaId, String email) {
        String lower = text.toLowerCase(Locale.ROOT);

        // 1. Verificación básica de palabras ofensivas de respaldo
        for (String word : LOCAL_BANNED_WORDS) {
            if (lower.contains(word)) {
                recordSecurityAlert("OBSCENE_CONTENT", rutaId, email);
                throw new DlpViolationException("El comentario contiene lenguaje inapropiado no permitido.");
            }
        }

        // 2. Verificación básica de PII (ej. teléfonos expuestos públicamente)
        if (LOCAL_PII_PHONE_PATTERN.matcher(text).find()) {
            recordSecurityAlert("DLP_PII_LEAK", rutaId, email);
            throw new DlpViolationException("El comentario contiene números telefónicos o datos personales no autorizados.");
        }
    }

    private void recordSecurityAlert(String violationType, Integer rutaId, String email) {
        MDC.put("event_type", "security_alert");
        MDC.put("threat_type", violationType);
        MDC.put("user_email", email != null ? email : "anonymous");
        MDC.put("route_id", String.valueOf(rutaId));
        MDC.put("http_status", "400");

        log.error("Alerta de seguridad: Intento de ataque/violacion detectado [{}] en reporte de ruta {}", violationType, rutaId);
    }
}
