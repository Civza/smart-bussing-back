package buss.smartbussingapi.security;

import buss.smartbussingapi.Reporte.ReporteRutaRepository;
import buss.smartbussingapi.Usuario.Rol;
import buss.smartbussingapi.Usuario.UsuarioRepository;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.SecurityContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.convention.TestBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * KAN-21 (T7) + KAN-11: Supabase (Google OAuth) access tokens are verified against the project's
 * JWKS. The JWKS is replaced by an in-memory key so the test never reaches the network.
 */
@ActiveProfiles("test")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
class SupabaseJwtIntegrationTest {

    @TestBean(name = "supabaseJwkSource")
    private JWKSource<SecurityContext> supabaseJwkSource;

    static JWKSource<SecurityContext> supabaseJwkSource() {
        return TestJwtFactory.supabaseJwkSource();
    }

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private UsuarioRepository usuarioRepository;
    @Autowired
    private ReporteRutaRepository reporteRutaRepository;
    @Autowired
    private JwtTokenService jwtTokenService;

    @BeforeEach
    void setUp() {
        AuthTestSupport auth = new AuthTestSupport(usuarioRepository, reporteRutaRepository, jwtTokenService);
        auth.deleteAllUsers();
        auth.createUser("supervisor@cetys.mx", Rol.ADMIN);
        auth.createUser("pasajero@gmail.com", Rol.USER);
    }

    private static String bearer(String token) {
        return "Bearer " + token;
    }

    @Test
    void googleAdminTokenCanUseAdminEndpoints() throws Exception {
        String token = TestJwtFactory.supabaseToken("supervisor@cetys.mx", "google");
        mockMvc.perform(get("/api/v1/user").header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.info").value("All users retrieved"));
    }

    @Test
    void googleUserTokenIsForbiddenOnAdminEndpoints() throws Exception {
        String token = TestJwtFactory.supabaseToken("pasajero@gmail.com", "google");
        mockMvc.perform(get("/api/v1/user").header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.info").value("Forbidden"));
    }

    @Test
    void meReturnsTheCallersProfileAndRoleForTheDashboard() throws Exception {
        String token = TestJwtFactory.supabaseToken("supervisor@cetys.mx", "google");
        mockMvc.perform(get("/api/v1/user/me").header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.response.email").value("supervisor@cetys.mx"))
                .andExpect(jsonPath("$.response.rol").value("ADMIN"))
                .andExpect(jsonPath("$.response.password").doesNotExist());
    }

    @Test
    void meIs404WhenSupabaseUserHasNoUsuarioRow() throws Exception {
        String token = TestJwtFactory.supabaseToken("nuevo@gmail.com", "google");
        mockMvc.perform(get("/api/v1/user/me").header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isNotFound());
    }

    @Test
    void supabaseEmailPasswordIdentityIsNotTrustedBecauseEmailMayBeUnverified() throws Exception {
        String token = TestJwtFactory.supabaseToken("supervisor@cetys.mx", "email");
        mockMvc.perform(get("/api/v1/user").header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void tokenSignedWithAKeyNotInTheJwksIs401() throws Exception {
        String token = TestJwtFactory.supabaseTokenSignedByForeignKey("supervisor@cetys.mx");
        mockMvc.perform(get("/api/v1/user").header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void expiredSupabaseTokenIs401() throws Exception {
        String token = TestJwtFactory.expiredSupabaseToken("supervisor@cetys.mx");
        mockMvc.perform(get("/api/v1/user").header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void supabaseTokenForAnotherAudienceIs401() throws Exception {
        String token = TestJwtFactory.supabaseTokenWithAudience("supervisor@cetys.mx", "anon");
        mockMvc.perform(get("/api/v1/user").header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isUnauthorized());
    }
}
