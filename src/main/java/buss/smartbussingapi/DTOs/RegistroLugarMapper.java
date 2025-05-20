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

        if(empresa1.isEmpty()) {
            Empresa empresa = new Empresa();

            if(registroLugar.getCorreo_empresa() == null || registroLugar.getCorreo_empresa().isEmpty()) {
                throw new IllegalArgumentException("El correo esta vacio");
            }

            if(registroLugar.getNombreLugar() == null || registroLugar.getNombreLugar().isEmpty()) {
                throw new IllegalArgumentException("El nombre del lugar esta vacio");
            }

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

        if(lugarDTO.getNombreLugar() == null || lugarDTO.getNombreLugar().isEmpty()) {
            throw new IllegalArgumentException("El nombre del lugar esta vacio");
        }

        if(lugarDTO.getTelefono() == null || lugarDTO.getTelefono().isEmpty()) {
            throw new IllegalArgumentException("El telefono del lugar esta vacio");
        }

        if(lugarDTO.getDescripcion() == null || lugarDTO.getDescripcion().isEmpty()) {
            throw new IllegalArgumentException("El descripcion del lugar esta vacio");
        }

        lugar.setName(lugarDTO.getNombreLugar());
        lugar.setTelefono(lugarDTO.getTelefono());
        lugar.setDescripcion(lugarDTO.getDescripcion());
        lugar.setTipo("Indefinido");
        lugar.setEmpresa(empresa);
        lugarRepository.save(lugar);
    }

}
