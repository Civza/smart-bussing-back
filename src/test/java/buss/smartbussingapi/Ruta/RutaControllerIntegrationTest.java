package buss.smartbussingapi.Ruta;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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

    // @BeforeEach se ejecuta antes de CADA prueba — garantiza que la BD inicia vacía en cada test
    @BeforeEach
    public void limpiarBD() {
        rutaRepository.deleteAll();
    }

    private Ruta guardarRutaEnDB(String nombre_ruta, String nombre_corto_ruta, String color_ruta, String color_texto_ruta, String tipo_ruta, String horario_ruta, boolean active) {
        return rutaRepository.save(new Ruta(null, nombre_ruta, nombre_corto_ruta, color_ruta, color_texto_ruta, tipo_ruta, horario_ruta, active, null, null, null));
    }

    @Test
    void shouldCreateRutaAndReturn201() throws Exception {
        String body = """ 
                {
                "nombre_ruta": "Ruta 1",
                "nombre_corto_ruta": "R1",
                "color_ruta": "red",
                "color_texto_ruta": "white",
                "tipo_ruta": "LineString",
                "horario_ruta": "10:00-18:00",
                "active": true
                   }""";

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
                .andExpect(jsonPath("$.response.horario_ruta").value("10:00-18:00"))
                .andExpect(jsonPath("$.response.active").value(true))
                .andExpect(jsonPath("$.response.id_ruta").exists())
                .andExpect(jsonPath("$.error").doesNotExist());
    }


}
