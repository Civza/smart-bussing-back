package buss.smartbussingapi.Parada;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
public class ParadaControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ParadaRepository paradaRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    public void limpiarBD() {
        paradaRepository.deleteAll();
    }

    @Test
    void shouldAddParadaAndReturn201() throws Exception {
        String jsonBody = """
                {
                    "nombre_parada": "Parada Central",
                    "zona_parada": "Centro",
                    "descripcion_parada": "Frente al parque",
                    "tiempo_Espera": 5
                }
                """;

        mockMvc.perform(post("/api/v1/parada")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.info").value("Parada created"));
    }

    @Test
    void shouldGetAllParadas() throws Exception {
        String jsonParada = """
                {
                    "nombre_parada": "Parada Central",
                    "zona_parada": "Centro",
                    "descripcion_parada": "Frente al parque",
                    "tiempo_Espera": 5
                }
                """;
        Parada parada = objectMapper.readValue(jsonParada, Parada.class);
        paradaRepository.save(parada);

        mockMvc.perform(get("/api/v1/parada"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.info").value("All paradas retrieved"))
                .andExpect(jsonPath("$.response[0].nombre_parada").value("Parada Central"));
    }

    @Test
    void shouldGetParadaById() throws Exception {
        String jsonParada = """
                {
                    "nombre_parada": "Parada Central",
                    "zona_parada": "Centro",
                    "descripcion_parada": "Frente al parque",
                    "tiempo_Espera": 5
                }
                """;
        Parada parada = objectMapper.readValue(jsonParada, Parada.class);
        parada = paradaRepository.save(parada);

        mockMvc.perform(get("/api/v1/parada/" + parada.getId_parada()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.info").value("Parada retrieved"))
                .andExpect(jsonPath("$.response.nombre_parada").value("Parada Central"));
    }

    @Test
    void shouldReturn404Or400WhenParadaNotFound() throws Exception {
        mockMvc.perform(get("/api/v1/parada/999"))
                .andExpect(status().is4xxClientError());
    }
}
