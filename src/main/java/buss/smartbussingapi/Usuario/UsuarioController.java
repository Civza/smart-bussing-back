package buss.smartbussingapi.Usuario;

import buss.smartbussingapi.DTOs.ChangePasswordRequest;
import buss.smartbussingapi.DTOs.LoginRequest;
import buss.smartbussingapi.DTOs.LoginResponse;
import buss.smartbussingapi.DTOs.RegisterRequest;
import buss.smartbussingapi.DTOs.UpdateNameRequest;
import buss.smartbussingapi.DTOs.UsuarioDTO;
import buss.smartbussingapi.commons.ApiResponse;
import buss.smartbussingapi.security.JwtTokenService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Access rules live in SecurityConfig. Methods that act on "the caller" take the email from the
 * verified token ({@code @AuthenticationPrincipal}), never from a path id or request parameter (KAN-22).
 */
@RestController
@RequestMapping(path = "api/v1/user")
public class UsuarioController {

    /** SpEL over the verified {@link org.springframework.security.oauth2.jwt.Jwt} principal. */
    static final String TOKEN_EMAIL = "claims['email']";

    private final UsuarioService usuarioService;
    private final RoleAuthorizationService roleAuthorizationService;
    private final JwtTokenService jwtTokenService;

    @Autowired
    public UsuarioController(UsuarioService usuarioService, RoleAuthorizationService roleAuthorizationService,
                             JwtTokenService jwtTokenService) {
        this.usuarioService = usuarioService;
        this.roleAuthorizationService = roleAuthorizationService;
        this.jwtTokenService = jwtTokenService;
    }

    @GetMapping
    @ResponseStatus(HttpStatus.OK)
    public ApiResponse<List<Usuario>> getUsuarios(@AuthenticationPrincipal(expression = TOKEN_EMAIL) String userEmail) {
        roleAuthorizationService.requireAdmin(userEmail);
        return new ApiResponse<>("All users retrieved", usuarioService.getUsuarios(), null);
    }

    @GetMapping("/me")
    @ResponseStatus(HttpStatus.OK)
    public ApiResponse<UsuarioDTO> getMe(@AuthenticationPrincipal(expression = TOKEN_EMAIL) String userEmail) {
        return new ApiResponse<>("User retrieved", usuarioService.getUsuarioByEmail(userEmail), null);
    }

    @GetMapping(path = "/{id_user}")
    @ResponseStatus(HttpStatus.OK)
    public ApiResponse<Usuario> getUsuarioById(@PathVariable("id_user") int idUser) {
        return new ApiResponse<>("User retrieved", usuarioService.getUsuarioById(idUser), null);
    }

    @GetMapping("/email")
    @ResponseStatus(HttpStatus.OK)
    public ApiResponse<UsuarioDTO> getUsuarioByEmail(@RequestParam String email) {
        return new ApiResponse<>("User retrieved", usuarioService.getUsuarioByEmail(email), null);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<Void> registerUser(@Valid @RequestBody RegisterRequest request) {
        Usuario usuario = new Usuario();
        usuario.setNombre(request.nombre());
        usuario.setEmail(request.email());
        usuario.setPassword(request.password());
        usuario.setProfilePhotoURL(request.profilePhotoURL());
        usuario.setRol(Rol.USER);
        usuarioService.registerUsuario(usuario);
        return new ApiResponse<>("User created", null, null);
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest request) {
        if (usuarioService.verifyCredential(request.email(), request.password())) {
            int id = usuarioService.getUsuarioIdByEmail(request.email());
            Usuario user = usuarioService.getUsuarioById(id);
            LoginResponse body = LoginResponse.bearer(jwtTokenService.issueToken(user),
                    jwtTokenService.getTtlSeconds(), toDto(user));
            return ResponseEntity.ok(new ApiResponse<>("Login successful", body, null));
        }
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(new ApiResponse<>("Unauthorized", null, "Invalid credentials"));
    }

    @PatchMapping(path = "/me/name")
    @ResponseStatus(HttpStatus.OK)
    public ApiResponse<Void> updateMyName(@AuthenticationPrincipal(expression = TOKEN_EMAIL) String userEmail,
                                          @Valid @RequestBody UpdateNameRequest request) {
        usuarioService.editProfileName(usuarioService.getUsuarioIdByEmail(userEmail), request.nombre());
        return new ApiResponse<>("Username updated", null, null);
    }

    @PatchMapping(path = "/me/password")
    @ResponseStatus(HttpStatus.OK)
    public ApiResponse<Void> changeMyPassword(@AuthenticationPrincipal(expression = TOKEN_EMAIL) String userEmail,
                                              @Valid @RequestBody ChangePasswordRequest request) {
        usuarioService.changePassword(usuarioService.getUsuarioIdByEmail(userEmail),
                request.currentPassword(), request.newPassword());
        return new ApiResponse<>("Password updated", null, null);
    }

    private static UsuarioDTO toDto(Usuario usuario) {
        UsuarioDTO dto = new UsuarioDTO();
        dto.setIdUsuario(usuario.getIdUsuario());
        dto.setNombre(usuario.getNombre());
        dto.setEmail(usuario.getEmail());
        dto.setUrlPhoto(usuario.getProfilePhotoURL());
        dto.setRol(usuario.getRol());
        return dto;
    }
}
