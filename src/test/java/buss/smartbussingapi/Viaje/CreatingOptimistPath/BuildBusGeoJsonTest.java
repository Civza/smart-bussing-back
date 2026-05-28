package buss.smartbussingapi.Viaje.CreatingOptimistPath;

import buss.smartbussingapi.Coordenadas.Coordenadas;
import buss.smartbussingapi.DTOs.GeoJsonRoute.GeoJsonRouteGeometry;
import buss.smartbussingapi.Parada.Parada;
import buss.smartbussingapi.Ruta.Ruta;
import buss.smartbussingapi.commons.exceptions.InvalidDataException;
import buss.smartbussingapi.commons.exceptions.NotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class BuildBusGeoJsonTest {

    private BuildBusGeoJson buildBusGeoJson;

    @BeforeEach
    void setUp() {
        buildBusGeoJson = new BuildBusGeoJson();
    }

    @Test
    void testBuildBusGeoJson_NullOrEmptyParadas_ThrowsInvalidDataException() {
        assertThrows(InvalidDataException.class, () -> buildBusGeoJson.buildBusGeoJson(null));
        assertThrows(InvalidDataException.class, () -> buildBusGeoJson.buildBusGeoJson(new ArrayList<>()));
        assertThrows(InvalidDataException.class, () -> buildBusGeoJson.buildBusGeoJson(List.of(new Parada())));
    }

    @Test
    void testBuildBusGeoJson_NoCommonRoute_ThrowsNotFoundException() {
        Ruta ruta1 = new Ruta();
        ruta1.setId_ruta(1);
        
        Ruta ruta2 = new Ruta();
        ruta2.setId_ruta(2);

        Parada origen = new Parada();
        origen.setNombre_parada("Origen");
        origen.setRutas(List.of(ruta1));

        Parada destino = new Parada();
        destino.setNombre_parada("Destino");
        destino.setRutas(List.of(ruta2)); // No shared route

        List<Parada> paradas = List.of(origen, destino);

        assertThrows(NotFoundException.class, () -> buildBusGeoJson.buildBusGeoJson(paradas));
    }

    @Test
    void testBuildBusGeoJson_Success() {
        Ruta ruta = new Ruta();
        ruta.setId_ruta(1);

        // Trayecto of the route
        Coordenadas c1 = new Coordenadas(10.0, -84.0, "IDA");
        Coordenadas c2 = new Coordenadas( 10.1, -84.1, "IDA");
        Coordenadas c3 = new Coordenadas( 10.2, -84.2, "IDA");
        Coordenadas c4 = new Coordenadas(10.3, -84.3, "IDA");
        ruta.setCoordenadas(List.of(c1, c2, c3, c4));

        Parada origen = new Parada();
        origen.setCoordenadas_parada(c2); // closest to index 1
        origen.setRutas(List.of(ruta));

        Parada destino = new Parada();
        destino.setCoordenadas_parada(c4); // closest to index 3
        destino.setRutas(List.of(ruta));

        List<Parada> paradas = List.of(origen, destino);

        GeoJsonRouteGeometry result = buildBusGeoJson.buildBusGeoJson(paradas);

        assertNotNull(result);
        assertEquals("LineString", result.getType());
        // Should contain c2, c3, c4
        assertEquals(3, result.getCoordinates().size());
        assertEquals(List.of(-84.1, 10.1), result.getCoordinates().get(0));
        assertEquals(List.of(-84.3, 10.3), result.getCoordinates().get(2));
    }

    @Test
    void testBuildBusGeoJson_Success_ReversedOrder() {
        Ruta ruta = new Ruta();
        ruta.setId_ruta(1);

        Coordenadas c1 = new Coordenadas(10.0, -84.0, "IDA");
        Coordenadas c2 = new Coordenadas(10.1, -84.1, "IDA");
        Coordenadas c3 = new Coordenadas(10.2, -84.2, "IDA");
        ruta.setCoordenadas(List.of(c1, c2, c3));

        // Origen closest to c3, destino closest to c1 (reversed indices)
        Parada origen = new Parada();
        origen.setCoordenadas_parada(c3);
        origen.setRutas(List.of(ruta));

        Parada destino = new Parada();
        destino.setCoordenadas_parada(c1);
        destino.setRutas(List.of(ruta));

        List<Parada> paradas = List.of(origen, destino);

        GeoJsonRouteGeometry result = buildBusGeoJson.buildBusGeoJson(paradas);

        assertNotNull(result);
        assertEquals(3, result.getCoordinates().size()); // c1, c2, c3 (Math.min, Math.max ensures right order)
    }
}
