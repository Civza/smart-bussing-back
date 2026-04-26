package buss.smartbussingapi.Usuario;

import buss.smartbussingapi.DTOs.UsuarioDTO;
import buss.smartbussingapi.commons.ApiResponse;
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
    @ResponseStatus(HttpStatus.OK)
    public ApiResponse<List<Usuario>> getUsuarios() {
        return new ApiResponse<>("All users retrieved", usuarioService.getUsuarios(), null);
    }

    @GetMapping(path = "/{id_user}")
    @ResponseStatus(HttpStatus.OK)
    public ApiResponse<Usuario> getUsuarioById(@PathVariable int id_user) {
        return new ApiResponse<>("User retrieved", usuarioService.getUsuarioById(id_user), null);
    }

    @GetMapping("/email")
    @ResponseStatus(HttpStatus.OK)
    public ApiResponse<UsuarioDTO> getUsuarioByEmail(@RequestParam String email) {
        return new ApiResponse<>("User retrieved", usuarioService.getUsuarioByEmail(email), null);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<Void> registerUser(@RequestBody Usuario usuario) {
        usuarioService.registerUsuario(usuario);
        return new ApiResponse<>("User created", null, null);
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestParam() String email, @RequestParam() String password) {
        if (usuarioService.verifyCredential(email, password)) {
            int id = usuarioService.getUsuarioIdByEmail(email);
            Usuario user = usuarioService.getUsuarioById(id);
            return ResponseEntity.ok(new ApiResponse<>("Login successful", user, null));
        }
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(new ApiResponse<>("Unauthorized", null, "Invalid credentials"));
    }

    @PatchMapping(path = "/mn/{id_user}")
    @ResponseStatus(HttpStatus.OK)
    public ApiResponse<Void> modifyUserName(@PathVariable("id_user") int id_user, @RequestParam String name) {
        usuarioService.editProfileName(id_user, name);
        return new ApiResponse<>("Username updated", null, null);
    }

    @PatchMapping(path = "/mp/{id_user}")
    @ResponseStatus(HttpStatus.OK)
    public ApiResponse<Void> modifyPassword(@PathVariable("id_user") int id_user, @RequestParam String password) {
        usuarioService.editPassword(id_user, password);
        return new ApiResponse<>("Password updated", null, null);
    }
}
