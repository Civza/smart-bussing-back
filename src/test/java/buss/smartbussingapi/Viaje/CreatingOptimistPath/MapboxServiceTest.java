package buss.smartbussingapi.Viaje.CreatingOptimistPath;

import buss.smartbussingapi.Coordenadas.Coordenadas;
import buss.smartbussingapi.DTOs.DirectionsResponse;
import buss.smartbussingapi.Parada.Parada;
import buss.smartbussingapi.Parada.ParadaService;
import buss.smartbussingapi.commons.exceptions.NotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatusCode;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.Collections;
import java.util.List;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MapboxServiceTest {

    @Mock
    private ParadaService paradaService;

    @Mock
    private WebClient webClient;

    @Mock
    private WebClient.RequestHeadersUriSpec requestHeadersUriSpec;

    @Mock
    private WebClient.RequestHeadersSpec requestHeadersSpec;

    @Mock
    private WebClient.ResponseSpec responseSpec;

    @InjectMocks
    private MapboxService mapboxService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(mapboxService, "webClient", webClient);
        ReflectionTestUtils.setField(mapboxService, "token", "test-token");
    }

    @Test
    void testFindNearestStop_EmptyStops_ThrowsNotFoundException() {
        when(paradaService.getParadasList()).thenReturn(Collections.emptyList());

        assertThrows(NotFoundException.class, () -> mapboxService.findNearestStop(10.0, -84.0));
    }

    @Test
    void testFindNearestStop_Success() {
        Parada p1 = new Parada();
        p1.setCoordenadas_parada(new Coordenadas(10.0, -84.0, "IDA")); // nearest

        Parada p2 = new Parada();
        p2.setCoordenadas_parada(new Coordenadas(10.5, -84.5, "IDA")); // far

        when(paradaService.getParadasList()).thenReturn(List.of(p1, p2));

        Parada nearest = mapboxService.findNearestStop(10.001, -84.001);

        assertEquals(p1, nearest);
    }

    @Test
    void testGetWalkingDirections_Success() {
        String mockJsonResponse = """
                {
                  "routes": [
                    {
                      "distance": 100.0,
                      "duration": 50.0,
                      "geometry": {
                        "type": "LineString",
                        "coordinates": [
                          [-84.0, 10.0],
                          [-84.001, 10.001]
                        ]
                      },
                      "legs": [
                        {
                          "steps": [
                            {
                              "maneuver": {
                                "instruction": "Walk forward"
                              }
                            }
                          ]
                        }
                      ]
                    }
                  ]
                }
                """;

        when(webClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri(any(Function.class))).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.onStatus(any(), any())).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(String.class)).thenReturn(Mono.just(mockJsonResponse));

        DirectionsResponse response = mapboxService.getWalkingDirections(10.0, -84.0, 10.001, -84.001);

        assertNotNull(response);
        assertEquals(100.0, response.getDistanceMeters());
        assertEquals(50.0, response.getTimeSeconds());
        assertEquals("LineString", response.getGeoJson().getType());
        assertEquals(2, response.getGeoJson().getCoordinates().size());
        assertEquals(1, response.getInstructions().size());
        assertEquals("Walk forward", response.getInstructions().get(0));
    }

    @Test
    void testGetWalkingDirections_NoRouteFound_ThrowsNotFoundException() {
        String mockJsonResponse = """
                {
                  "routes": []
                }
                """;

        when(webClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri(any(Function.class))).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.onStatus(any(), any())).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(String.class)).thenReturn(Mono.just(mockJsonResponse));

        assertThrows(NotFoundException.class, () -> mapboxService.getWalkingDirections(10.0, -84.0, 10.001, -84.001));
    }

    @Test
    void testGetWalkingDirectionsWithParadaOverloads() {
        Parada p = new Parada();
        p.setCoordenadas_parada(new Coordenadas(10.0, -84.0, "IDA"));
        
        String mockJsonResponse = """
                {
                  "routes": [
                    {
                      "distance": 100.0,
                      "duration": 50.0,
                      "geometry": {
                        "type": "LineString",
                        "coordinates": [
                          [-84.0, 10.0],
                          [-84.001, 10.001]
                        ]
                      },
                      "legs": [
                        {
                          "steps": [
                            {
                              "maneuver": {
                                "instruction": "Walk forward"
                              }
                            }
                          ]
                        }
                      ]
                    }
                  ]
                }
                """;

        when(webClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri(any(Function.class))).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.onStatus(any(), any())).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(String.class)).thenReturn(Mono.just(mockJsonResponse));

        // Overload 1
        DirectionsResponse response1 = mapboxService.getWalkingDirections(10.001, -84.001, p);
        assertNotNull(response1);
        
        // Overload 2
        DirectionsResponse response2 = mapboxService.getWalkingDirections(p, 10.001, -84.001);
        assertNotNull(response2);
    }

    @Test
    void testParseResponse_Exception() {
        String mockJsonResponse = "invalid_json";

        when(webClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri(any(Function.class))).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.onStatus(any(), any())).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(String.class)).thenReturn(Mono.just(mockJsonResponse));

        assertThrows(RuntimeException.class, () -> mapboxService.getWalkingDirections(10.0, -84.0, 10.001, -84.001));
    }

    @Test
    void testCheckTokenLoaded_Coverage() {
        ReflectionTestUtils.setField(mapboxService, "token", null);
        ReflectionTestUtils.invokeMethod(mapboxService, "checkTokenLoaded");
        
        ReflectionTestUtils.setField(mapboxService, "token", "");
        ReflectionTestUtils.invokeMethod(mapboxService, "checkTokenLoaded");
        
        ReflectionTestUtils.setField(mapboxService, "token", "valid-token");
        ReflectionTestUtils.invokeMethod(mapboxService, "checkTokenLoaded");
    }
}
