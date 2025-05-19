package buss.smartbussingapi.DTOs;

import buss.smartbussingapi.Contrato.Contrato;
import buss.smartbussingapi.Contrato.ContratoRepository;
import buss.smartbussingapi.Empresa.Empresa;
import buss.smartbussingapi.Empresa.EmpresaRepository;
import buss.smartbussingapi.Lugar.Lugar;
import buss.smartbussingapi.Lugar.LugarRepository;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class RegistroLugarMapper {

    private final ContratoRepository contratoRepository;
    private final EmpresaRepository empresaRepository;
    private final LugarRepository lugarRepository;

    public RegistroLugarMapper(ContratoRepository contratoRepository, EmpresaRepository empresaRepository, LugarRepository lugarRepository) {
        this.contratoRepository = contratoRepository;
        this.empresaRepository = empresaRepository;
        this.lugarRepository = lugarRepository;
    }

    public Empresa toEmpresa(RegistroLugarDTO registroLugar) {
        Optional<Empresa> empresa1 = empresaRepository.findByEmail(registroLugar.getCorreo_empresa());

        if(!empresa1.isPresent()) {
            Empresa empresa = new Empresa();
            empresa.setEmail(registroLugar.getCorreo_empresa());
            empresa.setName(registroLugar.getNombreEmpresa());
            empresa.setPais("Mexico");
            empresa.setMetodo_pago("Efectivo");
            empresaRepository.save(empresa);
            return empresa;
        }

        return empresa1.get();
    }

    /*
    public Contrato toContrato(RegistroLugarDTO registroLugar){
        Contrato contrato = new Contrato();
        contrato.setFecha_inicio(registroLugar.getFechaInicio());
        contrato.setFecha_fin(registroLugar.getFechaFin());
        contrato.setMonto(registroLugar.getMonto());
        contratoRepository.save(contrato);
        return contrato;
    }
    */


    public void toLugar(RegistroLugarDTO lugarDTO,Empresa empresa){
        Lugar lugar = new Lugar();
        lugar.setName(lugarDTO.getNombreLugar());
        lugar.setTelefono(lugarDTO.getTelefono());
        lugar.setDescripcion(lugarDTO.getDescripcion());
        lugar.setTipo(lugarDTO.getTipo());
        lugar.setEmpresa(empresa);

        lugarRepository.save(lugar);
    }

}
