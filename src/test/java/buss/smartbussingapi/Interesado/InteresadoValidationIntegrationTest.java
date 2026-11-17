package buss.smartbussingapi.Interesado;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** KAN-17 (T3): the public lead form validates its input; KAN-29 (T18): POST stays public. */
@ActiveProfiles("test")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
class InteresadoValidationIntegrationTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private InteresadoRepository interesadoRepository;

    @BeforeEach
    void setUp() {
        interesadoRepository.deleteAll();
    }

    @Test
    void invalidEmailIs400AndNothingIsStored() throws Exception {
        mockMvc.perform(post("/api/v1/interesado").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"esto-no-es-un-correo\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.info").value("Invalid data"));
        assertEquals(0, interesadoRepository.count());
    }

    @Test
    void anonymousVisitorCanLeaveTheirEmail() throws Exception {
        mockMvc.perform(post("/api/v1/interesado").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"visitante@test.com\"}"))
                .andExpect(status().isCreated());
        assertEquals(1, interesadoRepository.count());
    }
}
