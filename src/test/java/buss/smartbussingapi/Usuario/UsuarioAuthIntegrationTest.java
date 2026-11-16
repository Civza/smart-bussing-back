package buss.smartbussingapi.Usuario;

import buss.smartbussingapi.RateLimiterService;
import buss.smartbussingapi.Reporte.ReporteRutaRepository;
import buss.smartbussingapi.security.AuthTestSupport;
import buss.smartbussingapi.security.JwtTokenService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * KAN-15 (T1) no password in JSON, KAN-16 (T2) BCrypt, KAN-17 (T3) input validation / no role
 * mass-assignment, KAN-18 (T4) JSON credentials, KAN-22 (T8) /user/me instead of ids.
 */
@ActiveProfiles("test")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
class UsuarioAuthIntegrationTest {

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
    @Autowired
    private ObjectMapper objectMapper;

    private AuthTestSupport auth;

    @BeforeEach
    void setUp() {
        rateLimiterService.resetAll();
        auth = new AuthTestSupport(usuarioRepository, reporteRutaRepository, jwtTokenService);
        auth.deleteAllUsers();
    }

    private static String json(String nombre, String email, String password) {
        return "{\"nombre\":\"" + nombre + "\",\"email\":\"" + email + "\",\"password\":\"" + password + "\"}";
    }

    private static String login(String email, String password) {
        return "{\"email\":\"" + email + "\",\"password\":\"" + password + "\"}";
    }

    // ---- T3 registration ----

    @Test
    void registerIgnoresRequestedAdminRole() throws Exception {
        mockMvc.perform(post("/api/v1/user").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"Eve\",\"email\":\"eve@test.com\",\"password\":\"password123\",\"rol\":\"ADMIN\"}"))
                .andExpect(status().isCreated());
        assertEquals(Rol.USER, usuarioRepository.findByEmail("eve@test.com").orElseThrow().getRol());
    }

    @Test
    void registerWithInvalidEmailIs400() throws Exception {
        mockMvc.perform(post("/api/v1/user").contentType(MediaType.APPLICATION_JSON)
                        .content(json("Eve", "not-an-email", "password123")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.info").value("Invalid data"));
    }

    @Test
    void registerWithShortPasswordIs400() throws Exception {
        mockMvc.perform(post("/api/v1/user").contentType(MediaType.APPLICATION_JSON)
                        .content(json("Eve", "eve@test.com", "short")))
                .andExpect(status().isBadRequest());
    }

    @Test
    void registerWithBlankNameIs400() throws Exception {
        mockMvc.perform(post("/api/v1/user").contentType(MediaType.APPLICATION_JSON)
                        .content(json(" ", "eve@test.com", "password123")))
                .andExpect(status().isBadRequest());
    }

    // ---- T2 hashing ----

    @Test
    void registeredPasswordIsStoredAsBcrypt() throws Exception {
        mockMvc.perform(post("/api/v1/user").contentType(MediaType.APPLICATION_JSON)
                        .content(json("Eve", "eve@test.com", "password123")))
                .andExpect(status().isCreated());
        assertTrue(usuarioRepository.findByEmail("eve@test.com").orElseThrow().getPassword().startsWith("$2"));
    }

    @Test
    void legacyPlaintextUserCanLogInAndIsMigratedToBcrypt() throws Exception {
        Usuario legacy = new Usuario();
        legacy.setNombre("Legacy");
        legacy.setEmail("legacy@test.com");
        legacy.setPassword("password123");
        usuarioRepository.save(legacy);

        mockMvc.perform(post("/api/v1/user/login").contentType(MediaType.APPLICATION_JSON)
                        .content(login("legacy@test.com", "password123")))
                .andExpect(status().isOk());
        assertTrue(usuarioRepository.findByEmail("legacy@test.com").orElseThrow().getPassword().startsWith("$2"));
    }

    @Test
    void googleAccountCannotLogInWithOauthPlaceholder() throws Exception {
        Usuario google = new Usuario();
        google.setNombre("Google");
        google.setEmail("google@test.com");
        google.setPassword("OAUTH_GOOGLE");
        google.setRol(Rol.ADMIN);
        usuarioRepository.save(google);

        mockMvc.perform(post("/api/v1/user/login").contentType(MediaType.APPLICATION_JSON)
                        .content(login("google@test.com", "OAUTH_GOOGLE")))
                .andExpect(status().isUnauthorized());
    }

    // ---- T4 / T7 login ----

    @Test
    void loginWithJsonBodyReturnsBearerTokenAndProfileWithoutPassword() throws Exception {
        auth.createUser("ana@test.com", Rol.USER);

        String body = mockMvc.perform(post("/api/v1/user/login").contentType(MediaType.APPLICATION_JSON)
                        .content(login("ana@test.com", AuthTestSupport.PASSWORD)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.info").value("Login successful"))
                .andExpect(jsonPath("$.response.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.response.expiresIn").value(3600))
                .andExpect(jsonPath("$.response.usuario.email").value("ana@test.com"))
                .andExpect(jsonPath("$.response.usuario.rol").value("USER"))
                .andExpect(content().string(not(containsString("password"))))
                .andReturn().getResponse().getContentAsString();

        JsonNode node = objectMapper.readTree(body);
        String token = node.at("/response/token").asText();
        mockMvc.perform(get("/api/v1/user/me").header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.response.email").value("ana@test.com"));
    }

    @Test
    void loginWithWrongPasswordIs401() throws Exception {
        auth.createUser("ana@test.com", Rol.USER);
        mockMvc.perform(post("/api/v1/user/login").contentType(MediaType.APPLICATION_JSON)
                        .content(login("ana@test.com", "wrong-password")))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.info").value("Unauthorized"));
    }

    @Test
    void loginWithoutBodyIs400() throws Exception {
        mockMvc.perform(post("/api/v1/user/login").contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }

    // ---- T8 /user/me ----

    @Test
    void meRequiresAToken() throws Exception {
        mockMvc.perform(get("/api/v1/user/me")).andExpect(status().isUnauthorized());
    }

    @Test
    void meReturnsOnlyTheCallersProfile() throws Exception {
        Usuario ana = auth.createUser("ana@test.com", Rol.USER);
        auth.createUser("otro@test.com", Rol.USER);

        mockMvc.perform(get("/api/v1/user/me").header(HttpHeaders.AUTHORIZATION, auth.bearer(ana)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.response.email").value("ana@test.com"))
                .andExpect(jsonPath("$.response.rol").value("USER"))
                .andExpect(jsonPath("$.response.password").doesNotExist());
    }

    @Test
    void userRenamesOnlyThemselves() throws Exception {
        Usuario ana = auth.createUser("ana@test.com", Rol.USER);
        Usuario otro = auth.createUser("otro@test.com", Rol.USER);

        mockMvc.perform(patch("/api/v1/user/me/name").header(HttpHeaders.AUTHORIZATION, auth.bearer(ana))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"nombre\":\"Ana Maria\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.info").value("Username updated"));

        assertEquals("Ana Maria", usuarioRepository.findById(ana.getIdUsuario()).orElseThrow().getNombre());
        assertEquals("Usuario USER", usuarioRepository.findById(otro.getIdUsuario()).orElseThrow().getNombre());
    }

    @Test
    void renameRejectsBlankName() throws Exception {
        Usuario ana = auth.createUser("ana@test.com", Rol.USER);
        mockMvc.perform(patch("/api/v1/user/me/name").header(HttpHeaders.AUTHORIZATION, auth.bearer(ana))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"nombre\":\"\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void changePasswordRequiresCurrentPassword() throws Exception {
        Usuario ana = auth.createUser("ana@test.com", Rol.USER);
        mockMvc.perform(patch("/api/v1/user/me/password").header(HttpHeaders.AUTHORIZATION, auth.bearer(ana))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"currentPassword\":\"wrong-password\",\"newPassword\":\"newPassword456\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void changePasswordThenLoginWithNewPassword() throws Exception {
        Usuario ana = auth.createUser("ana@test.com", Rol.USER);
        mockMvc.perform(patch("/api/v1/user/me/password").header(HttpHeaders.AUTHORIZATION, auth.bearer(ana))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"currentPassword\":\"" + AuthTestSupport.PASSWORD + "\",\"newPassword\":\"newPassword456\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.info").value("Password updated"));

        mockMvc.perform(post("/api/v1/user/login").contentType(MediaType.APPLICATION_JSON)
                        .content(login("ana@test.com", "newPassword456")))
                .andExpect(status().isOk());
    }
}
