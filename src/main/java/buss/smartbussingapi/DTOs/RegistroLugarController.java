package buss.smartbussingapi.DTOs;

import buss.smartbussingapi.Contrato.Contrato;
import buss.smartbussingapi.Empresa.Empresa;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
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
     public ResponseEntity<?> registrarLugar(@RequestBody RegistroLugarDTO registroLugarDTO) {
         Empresa empresa = registroLugarMapper.toEmpresa(registroLugarDTO);
         Contrato contrato = registroLugarMapper.toContrato(registroLugarDTO);
         registroLugarMapper.toLugar(registroLugarDTO,empresa,contrato);

         return ResponseEntity.ok().build();
     }
}
