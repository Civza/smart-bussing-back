package buss.smartbussingapi.Interesado;

import buss.smartbussingapi.Usuario.Rol;
import buss.smartbussingapi.Usuario.Usuario;
import buss.smartbussingapi.Usuario.UsuarioRepository;
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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
@ActiveProfiles("test")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
public class InteresadoControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private InteresadoRepository interesadoRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private JwtTokenService jwtTokenService;

    private String adminBearer() {
        Usuario admin = usuarioRepository.findByEmail("interesado-admin@test.com").orElseGet(() -> {
            Usuario nuevo = new Usuario();
            nuevo.setNombre("Admin");
            nuevo.setEmail("interesado-admin@test.com");
            nuevo.setRol(Rol.ADMIN);
            return usuarioRepository.save(nuevo);
        });
        return "Bearer " + jwtTokenService.issueToken(admin);
    }

    @BeforeEach
    public void limpiarBD() {
        interesadoRepository.deleteAll();
    }

    @Test
    void shouldAddInteresadoAndReturn201() throws Exception {
        String jsonBody = """
                {
                    "email": "interesado@test.com"
                }
                """;

        mockMvc.perform(post("/api/v1/interesado")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.info").value("Interesado created"));
    }

    @Test
    void shouldGetAllInteresados() throws Exception {
        String jsonInteresado = """
                {
                    "email": "interesado@test.com"
                }
                """;
        Interesado interesado = objectMapper.readValue(jsonInteresado, Interesado.class);
        interesadoRepository.save(interesado);

        mockMvc.perform(get("/api/v1/interesado").header(HttpHeaders.AUTHORIZATION, adminBearer()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.info").value("All interesados retrieved"))
                .andExpect(jsonPath("$.response[0].email").value("interesado@test.com"));
    }

}
