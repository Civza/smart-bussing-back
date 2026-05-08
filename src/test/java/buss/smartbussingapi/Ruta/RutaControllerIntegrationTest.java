package buss.smartbussingapi.Ruta;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import com.fasterxml.jackson.databind.ObjectMapper;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
public class RutaControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    // Inyectamos el repository directamente para preparar datos de prueba sin pasar por el API
    @Autowired
    private RutaRepository rutaRepository;

    @Autowired
    private ObjectMapper objectMapper;

    // @BeforeEach se ejecuta antes de CADA prueba — garantiza que la BD inicia vacía en cada test
    @BeforeEach
    public void limpiarBD() {
        rutaRepository.deleteAll();
    }



    @Test
    void shouldCreateRutaAndReturn201() throws Exception {
        String body = """ 
                {
                  "type": "FeatureCollection",
                  "features": [
                    {
                      "type": "Feature",
                      "properties": {
                        "feature_type": "route",
                        "route_long_name": "Ruta 1",
                        "route_short_name": "R1",
                        "route_color": "red",
                        "route_text_color": "white",
                        "route_type": "LineString"
                      },
                      "geometry": {
                        "type": "LineString",
                        "coordinates": []
                      }
                    }
                  ]
                }
                """;

        mockMvc.perform(
                        post("/api/v1/ruta")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(body)
                )
                .andExpect(status().isCreated())                                    // HTTP 201
                .andExpect(jsonPath("$.info").value("Route created"))
                .andExpect(jsonPath("$.response.nombre_ruta").value("Ruta 1"))
                .andExpect(jsonPath("$.response.nombre_corto_ruta").value("R1"))
                .andExpect(jsonPath("$.response.color_ruta").value("red"))
                .andExpect(jsonPath("$.response.color_texto_ruta").value("white"))
                .andExpect(jsonPath("$.response.tipo_ruta").value("LineString"))
                .andExpect(jsonPath("$.response.active").value(true))
                .andExpect(jsonPath("$.response.id_ruta").exists())
                .andExpect(jsonPath("$.error").doesNotExist());
    }

    @Test
    void shouldGetAllRutas() throws Exception {
        String jsonRuta = """
                {
                    "nombre_ruta": "Ruta 1",
                    "nombre_corto_ruta": "R1",
                    "color_ruta": "red",
                    "color_texto_ruta": "white",
                    "tipo_ruta": "LineString",
                    "horario_ruta": "10:00-18:00",
                    "active": true
                }
                """;
        Ruta ruta = objectMapper.readValue(jsonRuta, Ruta.class);
        rutaRepository.save(ruta);

        mockMvc.perform(get("/api/v1/ruta"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.info").value("All routes retrieved"))
                .andExpect(jsonPath("$.response[0].nombre_ruta").value("Ruta 1"));
    }

    @Test
    void shouldGetRutaById() throws Exception {
        String jsonRuta = """
                {
                    "nombre_ruta": "Ruta 1",
                    "nombre_corto_ruta": "R1",
                    "color_ruta": "red",
                    "color_texto_ruta": "white",
                    "tipo_ruta": "LineString",
                    "horario_ruta": "10:00-18:00",
                    "active": true
                }
                """;
        Ruta ruta = objectMapper.readValue(jsonRuta, Ruta.class);
        ruta = rutaRepository.save(ruta);

        mockMvc.perform(get("/api/v1/ruta/" + ruta.getId_ruta()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.info").value("Route retrieved"))
                .andExpect(jsonPath("$.response.nombre_ruta").value("Ruta 1"));
    }

    @Test
    void shouldAddCoordenadasAndReturn201() throws Exception {
        String jsonRuta = """
                {
                    "nombre_ruta": "Ruta 1",
                    "nombre_corto_ruta": "R1",
                    "color_ruta": "red",
                    "color_texto_ruta": "white",
                    "tipo_ruta": "LineString",
                    "horario_ruta": "10:00-18:00",
                    "active": true
                }
                """;
        Ruta ruta = objectMapper.readValue(jsonRuta, Ruta.class);
        ruta = rutaRepository.save(ruta);

        String jsonCoordenadasBody = """
                [
                    {
                        "latitud": 10.123,
                        "longitud": -84.123
                    },
                    {
                        "latitud": 10.124,
                        "longitud": -84.124
                    }
                ]
                """;

        mockMvc.perform(
                        post("/api/v1/ruta/" + ruta.getId_ruta() + "/coor")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(jsonCoordenadasBody)
                )
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.info").value("Coordinates added"));
    }

    @Test
    void shouldGetCoordenadasByRuta() throws Exception {
        String jsonRuta = """
                {
                    "nombre_ruta": "Ruta 1",
                    "nombre_corto_ruta": "R1",
                    "color_ruta": "red",
                    "color_texto_ruta": "white",
                    "tipo_ruta": "LineString",
                    "horario_ruta": "10:00-18:00",
                    "active": true
                }
                """;
        Ruta ruta = objectMapper.readValue(jsonRuta, Ruta.class);
        ruta = rutaRepository.save(ruta);

        String jsonCoordenadasBody = """
                [
                    {
                        "latitud": 10.123,
                        "longitud": -84.123
                    }
                ]
                """;
        mockMvc.perform(
                        post("/api/v1/ruta/" + ruta.getId_ruta() + "/coor")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(jsonCoordenadasBody)
                )
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/v1/ruta/coordenadas/" + ruta.getId_ruta()))
                .andDo(org.springframework.test.web.servlet.result.MockMvcResultHandlers.print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.info").value("Coordinates retrieved"))
                .andExpect(jsonPath("$.response[0].latitud").value(10.123));
    }

    @Test
    void shouldReturn404Or400WhenRutaNotFound() throws Exception {
        mockMvc.perform(get("/api/v1/ruta/999"))
                // Expect status could be 404 or 400 depending on global exception handler for not found
                .andExpect(status().is4xxClientError());
    }

}
