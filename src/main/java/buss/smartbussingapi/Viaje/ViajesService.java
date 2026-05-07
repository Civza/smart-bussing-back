package buss.smartbussingapi.Viaje;

import buss.smartbussingapi.DTOs.ItineraryDTOs.ItineraryResponseDTO;
import buss.smartbussingapi.Parada.Parada;
import buss.smartbussingapi.Viaje.CreatingOptimistPath.MapboxService;
import buss.smartbussingapi.commons.exceptions.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

import static buss.smartbussingapi.commons.Methods.haversine;

@Service
public class ViajesService {

    @Autowired
    private final ViajeRepository viajeRepository;
    private final MapboxService mapboxService;

    public ViajesService(ViajeRepository viajeRepository, MapboxService mapboxService) {
        this.viajeRepository = viajeRepository;
        this.mapboxService = mapboxService;
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

        Parada p = mapboxService.findNearestStop(userLat,destLat);
        if(!isInTheRadio(userLat, userLon, p.getCoordenadas_parada().getLatitud(),p.getCoordenadas_parada().getLongitud(), 0.3)){

        }

        return null;
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
