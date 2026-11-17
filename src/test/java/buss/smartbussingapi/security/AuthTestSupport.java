package buss.smartbussingapi.security;

import buss.smartbussingapi.Reporte.ReporteRutaRepository;
import buss.smartbussingapi.Usuario.Rol;
import buss.smartbussingapi.Usuario.Usuario;
import buss.smartbussingapi.Usuario.UsuarioRepository;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

/** Creates users directly in the DB and issues real backend tokens for them. */
public class AuthTestSupport {

    public static final String PASSWORD = "password123";
    private static final BCryptPasswordEncoder BCRYPT = new BCryptPasswordEncoder();

    private final UsuarioRepository usuarioRepository;
    private final ReporteRutaRepository reporteRutaRepository;
    private final JwtTokenService jwtTokenService;

    public AuthTestSupport(UsuarioRepository usuarioRepository, ReporteRutaRepository reporteRutaRepository,
                           JwtTokenService jwtTokenService) {
        this.usuarioRepository = usuarioRepository;
        this.reporteRutaRepository = reporteRutaRepository;
        this.jwtTokenService = jwtTokenService;
    }

    /**
     * Reports reference users, so they must go first. Bulk deletes run immediately: with deleteAll()
     * inside a @Transactional test, Hibernate flushes the users created afterwards before the deletes,
     * and recreating an existing email violates usuario_email_key.
     */
    public void deleteAllUsers() {
        reporteRutaRepository.deleteAllInBatch();
        usuarioRepository.deleteAllInBatch();
    }

    public Usuario createUser(String email, Rol rol) {
        Usuario usuario = new Usuario();
        usuario.setNombre("Usuario " + rol);
        usuario.setEmail(email);
        usuario.setPassword(BCRYPT.encode(PASSWORD));
        usuario.setRol(rol);
        return usuarioRepository.save(usuario);
    }

    public String bearer(Usuario usuario) {
        return "Bearer " + jwtTokenService.issueToken(usuario);
    }
}
