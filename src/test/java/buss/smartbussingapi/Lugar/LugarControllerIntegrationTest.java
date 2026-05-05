package buss.smartbussingapi.Lugar;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
public class LugarControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private LugarRepository lugarRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    public void limpiarBD() {
        lugarRepository.deleteAll();
    }

    @Test
    void shouldGetAllLugares() throws Exception {
        String jsonLugar = """
                {
                    "name": "Lugar Central",
                    "tipo": "Restaurante",
                    "telefono": "12345678",
                    "descripcion": "Lugar de comida"
                }
                """;
        Lugar lugar = objectMapper.readValue(jsonLugar, Lugar.class);
        lugarRepository.save(lugar);

        mockMvc.perform(get("/api/v1/lugar"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.info").value("All lugares retrieved"))
                .andExpect(jsonPath("$.response[0].name").value("Lugar Central"));
    }

    @Test
    void shouldGetLugarById() throws Exception {
        String jsonLugar = """
                {
                    "name": "Lugar Central",
                    "tipo": "Restaurante",
                    "telefono": "12345678",
                    "descripcion": "Lugar de comida"
                }
                """;
        Lugar lugar = objectMapper.readValue(jsonLugar, Lugar.class);
        lugar = lugarRepository.save(lugar);

        mockMvc.perform(get("/api/v1/lugar/" + lugar.getLugar_id()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.info").value("Lugar retrieved"))
                .andExpect(jsonPath("$.response.name").value("Lugar Central"));
    }

    @Test
    void shouldReturn404Or400WhenLugarNotFound() throws Exception {
        mockMvc.perform(get("/api/v1/lugar/999"))
                .andExpect(status().is4xxClientError());
    }
}
