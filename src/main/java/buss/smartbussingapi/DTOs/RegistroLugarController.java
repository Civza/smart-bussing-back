package buss.smartbussingapi.DTOs;

import buss.smartbussingapi.Empresa.Empresa;
import buss.smartbussingapi.Usuario.RoleAuthorizationService;
import buss.smartbussingapi.commons.ApiResponse;
import buss.smartbussingapi.commons.exceptions.InvalidDataException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("api/v1/registrarLugar")
public class RegistroLugarController {

    private final RegistroLugarMapper registroLugarMapper;
    private final RoleAuthorizationService roleAuthorizationService;

    @Autowired
    public RegistroLugarController(RegistroLugarMapper registroLugarMapper, RoleAuthorizationService roleAuthorizationService) {
        this.registroLugarMapper = registroLugarMapper;
        this.roleAuthorizationService = roleAuthorizationService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.OK)
    public ApiResponse<RegistroLugarDTO> registrarLugar(
            @RequestHeader(value = "X-User-Email", required = false) String userEmail,
            @RequestBody RegistroLugarDTO registroLugarDTO) {
        roleAuthorizationService.requireAdmin(userEmail);
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
