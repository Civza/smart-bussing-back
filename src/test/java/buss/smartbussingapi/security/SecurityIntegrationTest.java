package buss.smartbussingapi.security;

import buss.smartbussingapi.RateLimiterService;
import buss.smartbussingapi.Reporte.ReporteRutaRepository;
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

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * KAN-21 (T7): bearer token validation for backend-issued tokens.
 * Missing / malformed / forged / expired / foreign-issuer tokens are 401; public reads stay open.
 */
@ActiveProfiles("test")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
class SecurityIntegrationTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private UsuarioRepository usuarioRepository;
    @Autowired
    private ReporteRutaRepository reporteRutaRepository;
    @Autowired
    private JwtTokenService jwtTokenService;
    @Autowired
    private RateLimiterService rateLimiterService;

    private AuthTestSupport auth;
    private Usuario admin;

    @BeforeEach
    void setUp() {
        rateLimiterService.resetAll();
        auth = new AuthTestSupport(usuarioRepository, reporteRutaRepository, jwtTokenService);
        auth.deleteAllUsers();
        admin = auth.createUser("admin@test.com", Rol.ADMIN);
    }

    @Test
    void adminEndpointWithoutTokenIs401WithApiResponseBody() throws Exception {
        mockMvc.perform(get("/api/v1/user"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.info").value("Unauthorized"))
                .andExpect(jsonPath("$.error").exists());
    }

    @Test
    void validAdminTokenIsAccepted() throws Exception {
        mockMvc.perform(get("/api/v1/user").header(HttpHeaders.AUTHORIZATION, auth.bearer(admin)))
                .andExpect(status().isOk());
    }

    @Test
    void malformedTokenIs401() throws Exception {
        mockMvc.perform(get("/api/v1/user").header(HttpHeaders.AUTHORIZATION, "Bearer not-a-jwt"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.info").value("Unauthorized"));
    }

    @Test
    void tokenSignedWithAnotherSecretIs401() throws Exception {
        String forged = TestJwtFactory.appTokenSignedWithWrongSecret(admin.getIdUsuario(), admin.getEmail());
        mockMvc.perform(get("/api/v1/user").header(HttpHeaders.AUTHORIZATION, "Bearer " + forged))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void expiredTokenIs401() throws Exception {
        String expired = TestJwtFactory.expiredAppToken(admin.getIdUsuario(), admin.getEmail());
        mockMvc.perform(get("/api/v1/user").header(HttpHeaders.AUTHORIZATION, "Bearer " + expired))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void tokenFromUnknownIssuerIs401() throws Exception {
        String foreign = TestJwtFactory.appTokenFromUnknownIssuer(admin.getIdUsuario(), admin.getEmail());
        mockMvc.perform(get("/api/v1/user").header(HttpHeaders.AUTHORIZATION, "Bearer " + foreign))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void spoofedXUserEmailHeaderNoLongerGrantsAdmin() throws Exception {
        mockMvc.perform(get("/api/v1/user").header("X-User-Email", "admin@test.com"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void tokenForDeletedUserGetsNoAdminRights() throws Exception {
        String token = auth.bearer(admin);
        auth.deleteAllUsers();
        mockMvc.perform(get("/api/v1/user").header(HttpHeaders.AUTHORIZATION, token))
                .andExpect(status().isForbidden());
    }

    @Test
    void roleIsReadFromTheDatabaseOnEveryRequest() throws Exception {
        String token = auth.bearer(admin);
        admin.setRol(Rol.USER);
        usuarioRepository.save(admin);
        mockMvc.perform(get("/api/v1/user").header(HttpHeaders.AUTHORIZATION, token))
                .andExpect(status().isForbidden());
    }

    @Test
    void publicTransitReadsNeedNoToken() throws Exception {
        mockMvc.perform(get("/api/v1/ruta")).andExpect(status().isOk());
        mockMvc.perform(get("/api/v1/parada")).andExpect(status().isOk());
        mockMvc.perform(get("/api/v1/lugar")).andExpect(status().isOk());
        mockMvc.perform(get("/api/v1/reporteRuta")).andExpect(status().isOk());
    }

    @Test
    void publicEndpointStillWorksWhenAnInvalidTokenIsNotSent() throws Exception {
        mockMvc.perform(post("/api/v1/interesado")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"lead@test.com\"}"))
                .andExpect(status().isCreated());
    }

    @Test
    void loginWithCredentialsInQueryStringIs400AndNeverEchoesThePassword() throws Exception {
        mockMvc.perform(post("/api/v1/user/login")
                        .queryParam("email", "admin@test.com")
                        .queryParam("password", AuthTestSupport.PASSWORD))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(not(containsString(AuthTestSupport.PASSWORD))));
    }

    @Test
    void corsPreflightFromDashboardOriginIsAllowedWithAuthorizationHeader() throws Exception {
        mockMvc.perform(options("/api/v1/ruta")
                        .header(HttpHeaders.ORIGIN, "http://localhost:5173")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS, "authorization,content-type"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "http://localhost:5173"));
    }
}
