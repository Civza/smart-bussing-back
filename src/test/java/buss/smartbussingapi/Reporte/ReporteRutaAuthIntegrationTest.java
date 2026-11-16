package buss.smartbussingapi.Reporte;

import buss.smartbussingapi.Ruta.Ruta;
import buss.smartbussingapi.Ruta.RutaRepository;
import buss.smartbussingapi.Usuario.Rol;
import buss.smartbussingapi.Usuario.Usuario;
import buss.smartbussingapi.Usuario.UsuarioRepository;
import buss.smartbussingapi.security.AuthTestSupport;
import buss.smartbussingapi.security.JwtTokenService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** KAN-22 (T8): report author comes from the token. KAN-15 (T1): reports never expose author email/password. */
@ActiveProfiles("test")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
class ReporteRutaAuthIntegrationTest {

    private static final String REPORT = """
            { "descripcion": "Camion lleno", "likeRoute": 3, "urlPhotos": [] }
            """;

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private UsuarioRepository usuarioRepository;
    @Autowired
    private ReporteRutaRepository reporteRutaRepository;
    @Autowired
    private RutaRepository rutaRepository;
    @Autowired
    private JwtTokenService jwtTokenService;

    private AuthTestSupport auth;
    private Ruta ruta;
    private Usuario ana;
    private Usuario victim;

    @BeforeEach
    void setUp() {
        auth = new AuthTestSupport(usuarioRepository, reporteRutaRepository, jwtTokenService);
        auth.deleteAllUsers();
        rutaRepository.deleteAll();
        Ruta nueva = new Ruta();
        nueva.setNombreRuta("Ruta Reportes");
        nueva.setActive(true);
        ruta = rutaRepository.save(nueva);
        ana = auth.createUser("ana@test.com", Rol.USER);
        victim = auth.createUser("victim@test.com", Rol.USER);
    }

    @Test
    void creatingAReportRequiresAToken() throws Exception {
        mockMvc.perform(post("/api/v1/reporteRuta/" + ruta.getIdRuta())
                        .param("email", "ana@test.com")
                        .contentType(MediaType.APPLICATION_JSON).content(REPORT))
                .andExpect(status().isUnauthorized());
        assertEquals(0, reporteRutaRepository.count());
    }

    @Test
    @Transactional
    void authorIsTheTokenSubjectEvenIfAnotherEmailIsPassed() throws Exception {
        mockMvc.perform(post("/api/v1/reporteRuta/" + ruta.getIdRuta())
                        .param("email", victim.getEmail())
                        .header(HttpHeaders.AUTHORIZATION, auth.bearer(ana))
                        .contentType(MediaType.APPLICATION_JSON).content(REPORT))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.response.descripcion").value("Camion lleno"));

        List<ReporteRuta> reportes = reporteRutaRepository.findAll();
        assertEquals(1, reportes.size());
        assertEquals("ana@test.com", reportes.get(0).getUsuario().getEmail());
    }

    @Test
    void publicReportListDoesNotExposeAuthorEmailOrPassword() throws Exception {
        mockMvc.perform(post("/api/v1/reporteRuta/" + ruta.getIdRuta())
                        .header(HttpHeaders.AUTHORIZATION, auth.bearer(ana))
                        .contentType(MediaType.APPLICATION_JSON).content(REPORT))
                .andExpect(status().isCreated())
                .andExpect(content().string(not(containsString("ana@test.com"))))
                .andExpect(content().string(not(containsString("password"))));

        mockMvc.perform(get("/api/v1/reporteRuta"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.response[0].descripcion").value("Camion lleno"))
                .andExpect(content().string(not(containsString("ana@test.com"))))
                .andExpect(content().string(not(containsString("password"))));
    }
}
