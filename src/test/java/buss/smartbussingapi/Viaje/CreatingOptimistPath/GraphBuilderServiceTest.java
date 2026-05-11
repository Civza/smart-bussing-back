package buss.smartbussingapi.Viaje.CreatingOptimistPath;

import buss.smartbussingapi.Coordenadas.Coordenadas;
import buss.smartbussingapi.Ruta.Ruta;
import buss.smartbussingapi.Ruta.RutaRepository;
import buss.smartbussingapi.Ruta.RutaType;
import org.jgrapht.graph.DefaultDirectedWeightedGraph;
import org.jgrapht.graph.DefaultWeightedEdge;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class GraphBuilderServiceTest {

    @Mock
    private RutaRepository rutaRepository;

    @InjectMocks
    private GraphBuilderService graphBuilderService;

    @Test
    void testRebuildGraph_WithTransfers() {
        // Setup two overlapping routes
        // Route 1: (0,0) -> (0, 0.01) -> (0, 0.02)
        // Route 2: (0.0001, 0) -> (0.0001, 0.01) -> (0.0001, 0.02)
        // These routes are very close (approx 11m apart)

        Ruta r1 = createMockRuta(1, "R1", 0.0, 0.0, 0.0, 0.01, 0.0, 0.02);
        Ruta r2 = createMockRuta(2, "R2", 0.0001, 0.0, 0.0001, 0.01, 0.0001, 0.02);

        when(rutaRepository.findAllActiveWithCoordenadas()).thenReturn(List.of(r1, r2));

        graphBuilderService.rebuildGraph();

        DefaultDirectedWeightedGraph<Integer, DefaultWeightedEdge> graph = graphBuilderService.getGraph();
        assertNotNull(graph);
        assertEquals(6, graph.vertexSet().size());

        // Check if there are transfer edges
        // Total edges = 4 (bus edges) + transfers
        assertTrue(graph.edgeSet().size() > 4, "Should have transfer edges");
        
        // Find a node from R1 and see if it can reach R2
        Integer nodeR1 = graphBuilderService.findNearestVertex(0.0, 0.0);
        Integer nodeR2 = graphBuilderService.findNearestVertex(0.0001, 0.0);
        
        assertNotNull(nodeR1);
        assertNotNull(nodeR2);
        assertNotEquals(nodeR1, nodeR2);
        
        assertTrue(graph.containsEdge(nodeR1, nodeR2), "Should have transfer from R1 to R2");
    }

    private Ruta createMockRuta(int id, String name, double... coords) {
        Ruta r = new Ruta();
        r.setId_ruta(id);
        r.setNombre_ruta(name);
        r.setActive(true);
        r.setBidirectional(true);
        r.setTipo_ruta(RutaType.MICROBUS);
        
        List<Coordenadas> polyline = new ArrayList<>();
        for (int i = 0; i < coords.length; i += 2) {
            Coordenadas c = new Coordenadas();
            c.setLatitud(coords[i]);
            c.setLongitud(coords[i+1]);
            c.setSentido("AMBOS");
            polyline.add(c);
        }
        r.setCoordenadas(polyline);
        return r;
    }
}
