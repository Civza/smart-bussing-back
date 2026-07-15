package buss.smartbussingapi.Reporte;

import buss.smartbussingapi.Ruta.Ruta;
import buss.smartbussingapi.Ruta.RutaRepository;
import buss.smartbussingapi.Usuario.Usuario;
import buss.smartbussingapi.Usuario.UsuarioRepository;
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
public class ReporteRutaControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ReporteRutaRepository reporteRutaRepository;

    @Autowired
    private RutaRepository rutaRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    public void limpiarBD() {
        reporteRutaRepository.deleteAll();
        rutaRepository.deleteAll();
        usuarioRepository.deleteAll();
    }

    @Test
    void shouldCreateNewReporteRutaAndReturn201() throws Exception {
        // Setup Ruta and Usuario
        String jsonRuta = """
                {
                    "nombre_ruta": "Ruta 1",
                    "nombre_corto_ruta": "R1"
                }
                """;
        Ruta ruta = objectMapper.readValue(jsonRuta, Ruta.class);
        ruta = rutaRepository.save(ruta);

        String jsonUsuario = """
                {
                    "email": "test@test.com",
                    "nombre": "User Test"
                }
                """;
        Usuario usuario = objectMapper.readValue(jsonUsuario, Usuario.class);
        usuarioRepository.save(usuario);

        String jsonBody = """
                {
                    "descripcion": "Reporte de prueba",
                    "likeRoute": 5,
                    "urlPhotos": []
                }
                """;

        mockMvc.perform(post("/api/v1/reporteRuta/" + ruta.getIdRuta())
                        .param("email", "test@test.com")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.response.descripcion").value("Reporte de prueba"));
    }

    @Test
    void shouldGetAllReportesRuta() throws Exception {
        String jsonReporte = """
                {
                    "descripcion": "Reporte de prueba",
                    "likeRoute": 5
                }
                """;
        ReporteRuta reporteRuta = objectMapper.readValue(jsonReporte, ReporteRuta.class);
        reporteRutaRepository.save(reporteRuta);

        mockMvc.perform(get("/api/v1/reporteRuta"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.info").value("All route reports retrieved"))
                .andExpect(jsonPath("$.response[0].descripcion").value("Reporte de prueba"));
    }

    @Test
    void shouldGetReporteRutaById() throws Exception {
        String jsonReporte = """
                {
                    "descripcion": "Reporte de prueba",
                    "likeRoute": 5
                }
                """;
        ReporteRuta reporteRuta = objectMapper.readValue(jsonReporte, ReporteRuta.class);
        reporteRuta = reporteRutaRepository.save(reporteRuta);

        mockMvc.perform(get("/api/v1/reporteRuta/" + reporteRuta.getIdReporteRuta()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.info").value("Route report retrieved"))
                .andExpect(jsonPath("$.response.descripcion").value("Reporte de prueba"));
    }

    @Test
    void shouldGetReporteRutaByRouteName() throws Exception {
        // Setup Ruta
        String jsonRuta = """
                {
                    "nombre_ruta": "RutaTest"
                }
                """;
        Ruta ruta = objectMapper.readValue(jsonRuta, Ruta.class);
        ruta = rutaRepository.save(ruta);

        String jsonReporte = """
                {
                    "descripcion": "Reporte de prueba",
                    "likeRoute": 5
                }
                """;
        ReporteRuta reporteRuta = objectMapper.readValue(jsonReporte, ReporteRuta.class);
        reporteRuta.setRuta(ruta);
        reporteRutaRepository.save(reporteRuta);

        mockMvc.perform(get("/api/v1/reporteRuta/byRuta").param("routeName", "RutaTest"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.info").value("Route reports retrieved"))
                .andExpect(jsonPath("$.response[0].descripcion").value("Reporte de prueba"));
    }

    @Test
    void shouldReturn404WhenReporteNotFound() throws Exception {
        mockMvc.perform(get("/api/v1/reporteRuta/999"))
                .andExpect(status().is4xxClientError());
    }

}
