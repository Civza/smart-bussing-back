package buss.smartbussingapi.Viaje;

import buss.smartbussingapi.DTOs.DirectionsResponse;
import buss.smartbussingapi.DTOs.GeoJsonRoute.GeoJsonRouteGeometry;
import buss.smartbussingapi.DTOs.ItineraryDTOs.ItineraryResponseDTO;
import buss.smartbussingapi.DTOs.ItineraryDTOs.SegmentoResponseDTO;
import buss.smartbussingapi.Parada.Parada;
import buss.smartbussingapi.Viaje.CreatingOptimistPath.AlgoService;
import buss.smartbussingapi.Viaje.CreatingOptimistPath.BuildBusGeoJson;
import buss.smartbussingapi.Viaje.CreatingOptimistPath.MapboxService;
import buss.smartbussingapi.commons.exceptions.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

import static buss.smartbussingapi.commons.Methods.haversine;

@Service
public class ViajesService {

    @Autowired
    private final ViajeRepository viajeRepository;
    private final MapboxService mapboxService;
    private final AlgoService algoService;
    private final BuildBusGeoJson busGeoJson;

    public ViajesService(ViajeRepository viajeRepository, MapboxService mapboxService, AlgoService algoService, BuildBusGeoJson busGeoJson) {
        this.viajeRepository = viajeRepository;
        this.mapboxService = mapboxService;
        this.algoService = algoService;
        this.busGeoJson = busGeoJson;
    }

    public Viaje getViajebyId(int id_viaje) {
        return viajeRepository.findById(id_viaje)
                .orElseThrow(() -> new NotFoundException("Viaje with ID " + id_viaje + " not found"));
    }

    public List<Viaje> getAllViajes(){
        return viajeRepository.findAll();
    }

    public ItineraryResponseDTO getDraftRoute(double userLat, double userLon, double destLat, double destLon){
        //TODO - PENDING LOGIC FOR TRAVEL - COMING ON ISSUE 17
        //1. Check if the nearest STOP is on a ratio of the initial
        //2. Id not get a DirectiosnAPI walking
        //3. Call the method on AlgoService
        //4. get the single route (Already delimited)
        //5. make the same steps 1 and 2 for the destination
        //6. Prepape the response on SegmentsDTO (TYPE, ROUTE)
        //8. Send it to Frontend

        ItineraryResponseDTO newItinerary = new ItineraryResponseDTO();
        List<SegmentoResponseDTO> segments = new ArrayList<>();
        double totalSeconds = 1800; //30 min default  - Pending wiht ML predcitin times
        double totalMeterts = 7200; //10 km for UABC - Cotsco distances.

        //Step 1 and 2
        Parada p = mapboxService.findNearestStop(userLat,userLon);
        if(!isInTheRadio(userLat, userLon, p.getCoordenadas_parada().getLatitud(),p.getCoordenadas_parada().getLongitud(), 0.3)){
            DirectionsResponse response = mapboxService.getWalkingDirections(userLat,userLon,p);
            SegmentoResponseDTO segmentoResponseDTO = SegmentoResponseDTO.builder()
                    .tipo("WALKING")
                    .descripcion("Caminar hasta la parada : " + p.getNombre_parada())
                    .directions(response)
                    .build();
            segments.add(segmentoResponseDTO);
            totalMeterts += response.getDistanceMeters();
            totalSeconds += response.getTimeSeconds();
        }

        //Step 3
        Parada dest = mapboxService.findNearestStop(destLat,destLon);
        List<Parada> segment = algoService.findOptimalRoute(p,dest);

        //Step 4
        GeoJsonRouteGeometry geoJsonRoute = busGeoJson.buildBusGeoJson(segment,6); //Ruta hardcodeada xd
        DirectionsResponse busDirections = DirectionsResponse.builder()
                .paradaDestino(dest)
                .geoJson(geoJsonRoute)
                .build();
        SegmentoResponseDTO busSegment = SegmentoResponseDTO.builder()
                .tipo("BUS")
                .descripcion("En el autobus cuida tus pertenencias")
                .directions(busDirections)
                .build();
        segments.add(busSegment);

        //Step 5
        if(!isInTheRadio(dest.getCoordenadas_parada().getLatitud(), dest.getCoordenadas_parada().getLongitud(), destLat,destLon, 0.3)){
            DirectionsResponse response = mapboxService.getWalkingDirections(userLat,userLon,p);
            SegmentoResponseDTO segmentoResponseDTO2 = SegmentoResponseDTO.builder()
                    .tipo("WALKING")
                    .descripcion("Caminar hasta la parada : " + p.getNombre_parada())
                    .directions(response)
                    .build();
            segments.add(segmentoResponseDTO2);
            totalMeterts += response.getDistanceMeters();
            totalSeconds += response.getTimeSeconds();
        }

        newItinerary.setDuracionTotalSegundos(totalSeconds);
        newItinerary.setDistanciaTotalMetros(totalMeterts);
        newItinerary.setSegmentos(segments);

        return newItinerary;
    }

    /*
    public Viaje createNewViaje(){

        return viajeRepository.save();
    }


     */

    //Check the ratio
    private boolean isInTheRadio(double userLat, double userLon, double stopLat, double stopLon, double radioKm){
        double distance = haversine(userLat, userLon, stopLat, stopLon);
        return distance <= radioKm;
    }

}
