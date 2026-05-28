package buss.smartbussingapi.Viaje;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
@ActiveProfiles("test")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
public class ViajeControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ViajeRepository viajeRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    public void limpiarBD() {
        viajeRepository.deleteAll();
    }

    @Test
    void shouldAddViajeAndReturn201() throws Exception {
        String jsonBody = """
                {
                    "tiempo_viaje": 60,
                    "costo_viaje": 150.50
                }
                """;

        mockMvc.perform(post("/api/v1/viaje")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.info").value("Viaje created"));
    }

    @Test
    void shouldGetAllViajes() throws Exception {
        String jsonViaje = """
                {
                    "tiempo_viaje": 60,
                    "costo_viaje": 150.50
                }
                """;
        Viaje viaje = objectMapper.readValue(jsonViaje, Viaje.class);
        viajeRepository.save(viaje);

        mockMvc.perform(get("/api/v1/viaje"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.info").value("All viajes retrieved"))
                .andExpect(jsonPath("$.response[0].tiempo_viaje").value(60));
    }

    @Test
    void shouldGetViajeById() throws Exception {
        String jsonViaje = """
                {
                    "tiempo_viaje": 60,
                    "costo_viaje": 150.50
                }
                """;
        Viaje viaje = objectMapper.readValue(jsonViaje, Viaje.class);
        viaje = viajeRepository.save(viaje);

        mockMvc.perform(get("/api/v1/viaje/" + viaje.getIdViaje()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.info").value("Viaje retrieved"))
                .andExpect(jsonPath("$.response.costo_viaje").value(150.50));
    }

    @Test
    void shouldReturn404Or400WhenViajeNotFound() throws Exception {
        mockMvc.perform(get("/api/v1/viaje/999"))
                .andExpect(status().is4xxClientError());
    }
}
