package buss.smartbussingapi.DTOs;

import buss.smartbussingapi.Empresa.Empresa;
import buss.smartbussingapi.commons.ApiResponse;
import buss.smartbussingapi.commons.exceptions.InvalidDataException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("api/v1/registrarLugar")
public class RegistroLugarController {

    @Autowired
    private final RegistroLugarMapper registroLugarMapper;

    public RegistroLugarController(RegistroLugarMapper registroLugarMapper) {
        this.registroLugarMapper = registroLugarMapper;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.OK)
    public ApiResponse<RegistroLugarDTO> registrarLugar(@RequestBody RegistroLugarDTO registroLugarDTO) {
        if (registroLugarDTO == null) {
            throw new InvalidDataException("El cuerpo de la solicitud no puede estar vacio");
        }
        if (registroLugarDTO.getNombreEmpresa() == null || registroLugarDTO.getNombreEmpresa().isBlank()) {
            throw new InvalidDataException("El nombre de la empresa es obligatorio");
        }
        if (registroLugarDTO.getNombreLugar() == null || registroLugarDTO.getNombreLugar().isBlank()) {
            throw new InvalidDataException("El nombre del lugar es obligatorio");
        }
        Empresa empresa = registroLugarMapper.toEmpresa(registroLugarDTO);
        //Contrato contrato = registroLugarMapper.toContrato(registroLugarDTO);
        registroLugarMapper.toLugar(registroLugarDTO, empresa);
        return new ApiResponse<>("Lugar y empresa registrados exitosamente", registroLugarDTO, null);
    }
}
