package buss.smartbussingapi.Usuario;

import buss.smartbussingapi.security.JwtTokenService;
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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
@ActiveProfiles("test")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
public class UsuarioControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JwtTokenService jwtTokenService;

    private String bearerFor(Usuario usuario) {
        return "Bearer " + jwtTokenService.issueToken(usuario);
    }

    private String adminBearer() {
        Usuario admin = new Usuario();
        admin.setNombre("Admin");
        admin.setEmail("admin@test.com");
        admin.setRol(Rol.ADMIN);
        return bearerFor(usuarioRepository.save(admin));
    }

    @BeforeEach
    public void limpiarBD() {
        usuarioRepository.deleteAll();
    }

    @Test
    void shouldRegisterUserAndReturn201() throws Exception {
        String jsonBody = """
                {
                    "nombre": "Test User",
                    "email": "test@test.com",
                    "password": "password123",
                    "profilePhotoURL": "http://photo.url"
                }
                """;

        mockMvc.perform(post("/api/v1/user")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.info").value("User created"));
    }

    @Test
    void shouldReturn401WhenGettingAllUsuariosWithoutToken() throws Exception {
        mockMvc.perform(get("/api/v1/user"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.info").value("Unauthorized"))
                .andExpect(jsonPath("$.error").exists());
    }

    @Test
    void shouldGetUsuarioById() throws Exception {
        String jsonUser = """
                {
                    "nombre": "Test User",
                    "email": "test@test.com",
                    "password": "password123"
                }
                """;
        Usuario user = objectMapper.readValue(jsonUser, Usuario.class);
        user = usuarioRepository.save(user);

        mockMvc.perform(get("/api/v1/user/" + user.getIdUsuario()).header(HttpHeaders.AUTHORIZATION, adminBearer()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.info").value("User retrieved"))
                .andExpect(jsonPath("$.response.nombre").value("Test User"));
    }

    @Test
    void shouldGetUsuarioByEmail() throws Exception {
        String jsonUser = """
                {
                    "nombre": "Test User",
                    "email": "test@test.com",
                    "password": "password123"
                }
                """;
        Usuario user = objectMapper.readValue(jsonUser, Usuario.class);
        usuarioRepository.save(user);

        mockMvc.perform(get("/api/v1/user/email").param("email", "test@test.com").header(HttpHeaders.AUTHORIZATION, adminBearer()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.info").value("User retrieved"))
                .andExpect(jsonPath("$.response.email").value("test@test.com"));
    }

    @Test
    void shouldLoginSuccessfully() throws Exception {
        String jsonUser = """
                {
                    "nombre": "Test User",
                    "email": "login@test.com",
                    "password": "password123"
                }
                """;
        Usuario user = objectMapper.readValue(jsonUser, Usuario.class);
        usuarioRepository.save(user);

        mockMvc.perform(post("/api/v1/user/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"login@test.com\",\"password\":\"password123\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.info").value("Login successful"))
                .andExpect(jsonPath("$.response.usuario.email").value("login@test.com"))
                .andExpect(jsonPath("$.response.token").exists());
    }

    @Test
    void shouldFailLoginWithInvalidCredentials() throws Exception {
        String jsonUser = """
                {
                    "nombre": "Test User",
                    "email": "login@test.com",
                    "password": "password123"
                }
                """;
        Usuario user = objectMapper.readValue(jsonUser, Usuario.class);
        usuarioRepository.save(user);

        mockMvc.perform(post("/api/v1/user/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"login@test.com\",\"password\":\"wrongpassword\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.info").value("Unauthorized"));
    }

    @Test
    void shouldModifyUserName() throws Exception {
        String jsonUser = """
                {
                    "nombre": "Test User",
                    "email": "test@test.com",
                    "password": "password123"
                }
                """;
        Usuario user = objectMapper.readValue(jsonUser, Usuario.class);
        user = usuarioRepository.save(user);

        mockMvc.perform(patch("/api/v1/user/me/name")
                        .header(HttpHeaders.AUTHORIZATION, bearerFor(user))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"New Name\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.info").value("Username updated"));
    }

    @Test
    void shouldModifyPassword() throws Exception {
        String jsonUser = """
                {
                    "nombre": "Test User",
                    "email": "test@test.com",
                    "password": "password123"
                }
                """;
        Usuario user = objectMapper.readValue(jsonUser, Usuario.class);
        user = usuarioRepository.save(user);

        mockMvc.perform(patch("/api/v1/user/me/password")
                        .header(HttpHeaders.AUTHORIZATION, bearerFor(user))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"currentPassword\":\"password123\",\"newPassword\":\"newpassword\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.info").value("Password updated"));
    }

    @Test
    void shouldReturn404WhenUserNotFoundById() throws Exception {
        mockMvc.perform(get("/api/v1/user/999"))
                .andExpect(status().is4xxClientError());
    }

    @Test
    void shouldReturn404WhenGetUsuarioByEmailNotFound() throws Exception {
        mockMvc.perform(get("/api/v1/user/email").param("email", "nonexistent@test.com"))
                .andExpect(status().is4xxClientError());
    }

    @Test
    void shouldFailToRegisterWhenEmailExists() throws Exception {
        String jsonUser = """
                {
                    "nombre": "Test User",
                    "email": "test@test.com",
                    "password": "password123"
                }
                """;
        Usuario user = objectMapper.readValue(jsonUser, Usuario.class);
        usuarioRepository.save(user);

        String jsonBody = """
                {
                    "nombre": "New User",
                    "email": "test@test.com",
                    "password": "password123",
                    "profilePhotoURL": "http://photo.url"
                }
                """;
        mockMvc.perform(post("/api/v1/user")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonBody))
                .andExpect(status().is4xxClientError());
    }
}
