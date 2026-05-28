package buss.smartbussingapi.Usuario;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
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
    void shouldGetAllUsuarios() throws Exception {
        String jsonUser = """
                {
                    "nombre": "Test User",
                    "email": "test@test.com",
                    "password": "password123"
                }
                """;
        Usuario user = objectMapper.readValue(jsonUser, Usuario.class);
        usuarioRepository.save(user);

        mockMvc.perform(get("/api/v1/user"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.info").value("All users retrieved"))
                .andExpect(jsonPath("$.response[0].nombre").value("Test User"));
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

        mockMvc.perform(get("/api/v1/user/" + user.getId_usuario()))
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

        mockMvc.perform(get("/api/v1/user/email").param("email", "test@test.com"))
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
                        .param("email", "login@test.com")
                        .param("password", "password123"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.info").value("Login successful"))
                .andExpect(jsonPath("$.response.email").value("login@test.com"));
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
                        .param("email", "login@test.com")
                        .param("password", "wrongpassword"))
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

        mockMvc.perform(patch("/api/v1/user/mn/" + user.getId_usuario())
                        .param("name", "New Name"))
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

        mockMvc.perform(patch("/api/v1/user/mp/" + user.getId_usuario())
                        .param("password", "newpassword"))
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
