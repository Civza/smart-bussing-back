package buss.smartbussingapi.Usuario;

import buss.smartbussingapi.DTOs.UsuarioDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping(path = "api/v1/user")
public class UsuarioController {

    @Autowired
    private final UsuarioService usuarioService;
    private UsuarioRepository usuarioRepository;

    public UsuarioController(UsuarioService usuarioService) {
        this.usuarioService = usuarioService;
    }

    @GetMapping
    public List<Usuario> getUsuarios() {
        return usuarioService.getUsuarios();
    }

    @GetMapping(path = "/{id_user}")
    public Usuario getUsuarioById(@PathVariable int id_user){
        return usuarioService.getUsuarioById(id_user);
    }

    @GetMapping("/email")
    public UsuarioDTO getUsuarioByEmail(@RequestParam String email){
        return usuarioService.getUsuarioByEmail(email);
    }

    @PostMapping
    public void registerUser(@RequestBody Usuario usuario){
        usuarioService.registerUsuario(usuario);
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestParam() String email, @RequestParam() String password){

        if(usuarioService.verifyCredential(email, password)){
            int id = usuarioService.getUsuarioIdByEmail(email);
            Usuario user = usuarioService.getUsuarioById(id);

            return ResponseEntity.ok(user);
        }

        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
    }

    @PatchMapping(path = "/mn/{id_user}")
    public void modifyUserName(@PathVariable("id_user") int id_user, @RequestParam String name){
        usuarioService.editProfileName(id_user, name);
    }

    @PatchMapping(path = "/mp/{id_user}")
    public void modifyPassword(@PathVariable("id_user") int id_user, @RequestParam String password){
        usuarioService.editPassword(id_user, password);
    }
}
