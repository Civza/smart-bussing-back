package buss.smartbussingapi.Reporte;

import buss.smartbussingapi.commons.exceptions.DlpViolationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpEntity;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DlpValidationServiceTest {

    @Mock
    private RestTemplate restTemplate;

    private DlpValidationService dlpValidationService;

    @BeforeEach
    void setUp() {
        dlpValidationService = new DlpValidationService(restTemplate, "http://localhost:8000/api/dlp/scan");
    }

    @Test
    void validateComment_CleanComment_Passes() {
        Map<String, Object> mockResponse = Map.of(
                "is_safe", true,
                "anonymized_text", "Excelente servicio en esta ruta"
        );

        when(restTemplate.postForObject(eq("http://localhost:8000/api/dlp/scan"), any(HttpEntity.class), eq(Map.class)))
                .thenReturn(mockResponse);

        assertDoesNotThrow(() ->
                dlpValidationService.validateComment("Excelente servicio en esta ruta", 1, "usuario@cetys.mx")
        );
    }

    @Test
    void validateComment_ObsceneComment_ThrowsDlpViolationException() {
        Map<String, Object> mockResponse = Map.of(
                "is_safe", false,
                "violation_type", "OBSCENE_CONTENT",
                "anonymized_text", "El servicio es una ****"
        );

        when(restTemplate.postForObject(eq("http://localhost:8000/api/dlp/scan"), any(HttpEntity.class), eq(Map.class)))
                .thenReturn(mockResponse);

        DlpViolationException ex = assertThrows(DlpViolationException.class, () ->
                dlpValidationService.validateComment("El chofer fue grosero", 1, "usuario@cetys.mx")
        );

        assertTrue(ex.getMessage().contains("OBSCENE_CONTENT"));
    }

    @Test
    void validateComment_MicroserviceOffline_FallbackCatchesBannedWords() {
        when(restTemplate.postForObject(eq("http://localhost:8000/api/dlp/scan"), any(HttpEntity.class), eq(Map.class)))
                .thenThrow(new RestClientException("Connection refused"));

        assertThrows(DlpViolationException.class, () ->
                dlpValidationService.validateComment("Este conductor es un incompetente total", 1, "usuario@cetys.mx")
        );
    }

    @Test
    void validateComment_MicroserviceOffline_FallbackCatchesPiiPhone() {
        when(restTemplate.postForObject(eq("http://localhost:8000/api/dlp/scan"), any(HttpEntity.class), eq(Map.class)))
                .thenThrow(new RestClientException("Connection refused"));

        assertThrows(DlpViolationException.class, () ->
                dlpValidationService.validateComment("Llamenme a mi numero personal 646-123-4567 para mas detalles", 1, "usuario@cetys.mx")
        );
    }

    @Test
    void validateComment_MicroserviceOffline_FallbackAllowsCleanComment() {
        when(restTemplate.postForObject(eq("http://localhost:8000/api/dlp/scan"), any(HttpEntity.class), eq(Map.class)))
                .thenThrow(new RestClientException("Connection refused"));

        assertDoesNotThrow(() ->
                dlpValidationService.validateComment("Ruta puntual y limpia", 1, "usuario@cetys.mx")
        );
    }

    @Test
    void validateComment_NullOrBlankText_DoesNothing() {
        assertDoesNotThrow(() -> dlpValidationService.validateComment(null, 1, "usuario@cetys.mx"));
        assertDoesNotThrow(() -> dlpValidationService.validateComment("   ", 1, "usuario@cetys.mx"));
    }
}
