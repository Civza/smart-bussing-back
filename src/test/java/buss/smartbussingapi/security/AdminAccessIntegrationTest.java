package buss.smartbussingapi.security;

import buss.smartbussingapi.RateLimiterService;
import buss.smartbussingapi.Interesado.Interesado;
import buss.smartbussingapi.Interesado.InteresadoRepository;
import buss.smartbussingapi.Parada.ParadaRepository;
import buss.smartbussingapi.Reporte.ReporteRutaRepository;
import buss.smartbussingapi.Ruta.RutaRepository;
import buss.smartbussingapi.Usuario.Rol;
import buss.smartbussingapi.Usuario.Usuario;
import buss.smartbussingapi.Usuario.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * KAN-30 (T19), KAN-29 (T18), KAN-22 (T8): every protected endpoint is checked for
 * anonymous (401), USER (403) and ADMIN (2xx).
 */
@ActiveProfiles("test")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
class AdminAccessIntegrationTest {

    private static final String RUTA_BODY = """
            {
              "type": "FeatureCollection",
              "features": [
                {
                  "type": "Feature",
                  "properties": {
                    "feature_type": "route",
                    "route_long_name": "Ruta Admin",
                    "route_short_name": "RA",
                    "route_color": "red",
                    "route_text_color": "white",
                    "route_type": "URBANA"
                  },
                  "geometry": { "type": "LineString", "coordinates": [] }
                }
              ]
            }
            """;

    private static final String PARADA_BODY = """
            {
              "type": "Feature",
              "geometry": { "type": "Point", "coordinates": [-116.5963, 31.8667] },
              "properties": {
                "feature_type": "stop",
                "stop_name": "Parada Admin",
                "stop_description": "Frente al parque"
              }
            }
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
    private ParadaRepository paradaRepository;
    @Autowired
    private InteresadoRepository interesadoRepository;
    @Autowired
    private JwtTokenService jwtTokenService;
    @Autowired
    private RateLimiterService rateLimiterService;

    private AuthTestSupport auth;
    private Usuario admin;
    private Usuario user;

    @BeforeEach
    void setUp() {
        rateLimiterService.resetAll();
        auth = new AuthTestSupport(usuarioRepository, reporteRutaRepository, jwtTokenService);
        auth.deleteAllUsers();
        rutaRepository.deleteAll();
        paradaRepository.deleteAll();
        interesadoRepository.deleteAll();
        admin = auth.createUser("admin@test.com", Rol.ADMIN);
        user = auth.createUser("user@test.com", Rol.USER);
    }

    private void expectMatrix(MockHttpServletRequestBuilder anon,
                              MockHttpServletRequestBuilder asUser,
                              MockHttpServletRequestBuilder asAdmin,
                              int adminStatus) throws Exception {
        mockMvc.perform(anon)
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.info").value("Unauthorized"));
        mockMvc.perform(asUser.header(HttpHeaders.AUTHORIZATION, auth.bearer(user)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.info").value("Forbidden"));
        mockMvc.perform(asAdmin.header(HttpHeaders.AUTHORIZATION, auth.bearer(admin)))
                .andExpect(status().is(adminStatus));
    }

    private static MockHttpServletRequestBuilder postJson(String url, String body) {
        return post(url).contentType(MediaType.APPLICATION_JSON).content(body);
    }

    @Test
    void createRuta() throws Exception {
        expectMatrix(postJson("/api/v1/ruta", RUTA_BODY), postJson("/api/v1/ruta", RUTA_BODY),
                postJson("/api/v1/ruta", RUTA_BODY), 201);
        assertEquals(1, rutaRepository.count());
    }

    @Test
    void createParada() throws Exception {
        expectMatrix(postJson("/api/v1/parada", PARADA_BODY), postJson("/api/v1/parada", PARADA_BODY),
                postJson("/api/v1/parada", PARADA_BODY), 201);
        assertEquals(1, paradaRepository.count());
    }

    @Test
    void listUsuarios() throws Exception {
        expectMatrix(get("/api/v1/user"), get("/api/v1/user"), get("/api/v1/user"), 200);
        mockMvc.perform(get("/api/v1/user").header(HttpHeaders.AUTHORIZATION, auth.bearer(admin)))
                .andExpect(jsonPath("$.response.length()").value(2))
                .andExpect(jsonPath("$.response[0].password").doesNotExist());
    }

    @Test
    void getUsuarioById() throws Exception {
        String url = "/api/v1/user/" + user.getIdUsuario();
        expectMatrix(get(url), get(url), get(url), 200);
        mockMvc.perform(get(url).header(HttpHeaders.AUTHORIZATION, auth.bearer(admin)))
                .andExpect(jsonPath("$.response.email").value("user@test.com"))
                .andExpect(jsonPath("$.response.password").doesNotExist());
    }

    @Test
    void getUsuarioByIdNotFoundForAdminIs404() throws Exception {
        mockMvc.perform(get("/api/v1/user/999999").header(HttpHeaders.AUTHORIZATION, auth.bearer(admin)))
                .andExpect(status().isNotFound());
    }

    @Test
    void getUsuarioByEmail() throws Exception {
        expectMatrix(get("/api/v1/user/email").param("email", "user@test.com"),
                get("/api/v1/user/email").param("email", "user@test.com"),
                get("/api/v1/user/email").param("email", "user@test.com"), 200);
    }

    @Test
    void getUsuarioByEmailNotFoundForAdminIs404() throws Exception {
        mockMvc.perform(get("/api/v1/user/email").param("email", "nobody@test.com")
                        .header(HttpHeaders.AUTHORIZATION, auth.bearer(admin)))
                .andExpect(status().isNotFound());
    }

    @Test
    void listInteresados() throws Exception {
        interesadoRepository.save(new Interesado("lead@test.com"));
        expectMatrix(get("/api/v1/interesado"), get("/api/v1/interesado"), get("/api/v1/interesado"), 200);
        mockMvc.perform(get("/api/v1/interesado").header(HttpHeaders.AUTHORIZATION, auth.bearer(admin)))
                .andExpect(jsonPath("$.response[0].email").value("lead@test.com"));
    }

    @Test
    void legacyRenameByIdEndpointCannotBeUsedToModifyAnotherUser() throws Exception {
        mockMvc.perform(patch("/api/v1/user/mn/" + admin.getIdUsuario()).param("name", "hacked")
                        .header(HttpHeaders.AUTHORIZATION, auth.bearer(user)))
                .andExpect(status().isForbidden());
        assertEquals("Usuario ADMIN", usuarioRepository.findById(admin.getIdUsuario()).orElseThrow().getNombre());
    }

    @Test
    void legacyPasswordByIdEndpointCannotBeUsedAnonymously() throws Exception {
        mockMvc.perform(patch("/api/v1/user/mp/" + admin.getIdUsuario()).param("password", "hacked123"))
                .andExpect(status().is4xxClientError());
        mockMvc.perform(post("/api/v1/user/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"admin@test.com\",\"password\":\"" + AuthTestSupport.PASSWORD + "\"}"))
                .andExpect(status().isOk());
    }
}
