package buss.smartbussingapi.DTOs;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
public class RegistroLugarControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void shouldRegistrarLugarAndReturn200() throws Exception {
        String jsonBody = """
                {
                    "nombreEmpresa": "Empresa Test",
                    "correo_empresa": "empresa@test.com",
                    "nombreLugar": "Sede Central",
                    "descripcion": "Sede principal",
                    "telefono": "12345678",
                    "tipo": "Restaurante"
                }
                """;

        mockMvc.perform(post("/api/v1/registrarLugar")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonBody))
                .andExpect(status().isOk());
    }
}
