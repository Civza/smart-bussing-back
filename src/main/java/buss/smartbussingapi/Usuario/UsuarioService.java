package buss.smartbussingapi.Usuario;

import buss.smartbussingapi.DTOs.UsuarioDTO;
import buss.smartbussingapi.commons.exceptions.AlreadyExistsException;
import buss.smartbussingapi.commons.exceptions.InvalidCredentialsException;
import buss.smartbussingapi.commons.exceptions.InvalidDataException;
import buss.smartbussingapi.commons.exceptions.NotFoundException;
import buss.smartbussingapi.commons.exceptions.ResourceBlockedException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class UsuarioService {

    public static final int MAX_FAILED_ATTEMPTS = 5;
    public static final long LOCKOUT_DURATION_MILLIS = 15 * 60 * 1000L; // 15 minutos

    private final Map<String, Integer> failedAttempts = new ConcurrentHashMap<>();
    private final Map<String, Long> lockoutExpiry = new ConcurrentHashMap<>();

    /** Placeholder the old Supabase trigger stored for Google accounts; it must never work as a password. */
    static final String OAUTH_PLACEHOLDER = "OAUTH_GOOGLE";
    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @Autowired
    private final UsuarioRepository usuarioRepository;

    public UsuarioService(UsuarioRepository usuarioRepository) {
        this.usuarioRepository = usuarioRepository;
    }

    public List<Usuario> getUsuarios() {
        return usuarioRepository.findAll();
    }

    public Usuario getUsuarioById(int idUsuario) {
        return usuarioRepository.findById(idUsuario)
                .orElseThrow(() -> new NotFoundException("User with ID " + idUsuario + " not found"));
    }

    public UsuarioDTO getUsuarioByEmail(String email) {
        Usuario usuarioMain = usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new NotFoundException("User with email " + email + " not found"));

        UsuarioDTO usuarioDTO = new UsuarioDTO();
        usuarioDTO.setIdUsuario(usuarioMain.getIdUsuario());
        usuarioDTO.setNombre(usuarioMain.getNombre());
        usuarioDTO.setEmail(usuarioMain.getEmail());
        usuarioDTO.setUrlPhoto(usuarioMain.getProfilePhotoURL());
        usuarioDTO.setRol(usuarioMain.getRol());
        return usuarioDTO;
    }

    public int getUsuarioIdByEmail(String email) {
        return usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new NotFoundException("User with email " + email + " not found"))
                .getIdUsuario();
    }

    public boolean verifyCredential(String correo, String password) {
        long now = System.currentTimeMillis();

        // Verificar si la cuenta esta temporalmente bloqueada por ataques de fuerza bruta
        Long expiry = lockoutExpiry.get(correo);
        if (expiry != null) {
            if (now < expiry) {
                throw new ResourceBlockedException(
                        "La cuenta se encuentra temporalmente bloqueada por multiples intentos fallidos. Intente de nuevo mas tarde."
                );
            } else {
                // El tiempo de bloqueo ya expiro, se remueve el bloqueo
                lockoutExpiry.remove(correo);
                failedAttempts.remove(correo);
            }
        }

        Usuario usuario = usuarioRepository.findByEmail(correo)
                .orElseThrow(() -> new InvalidCredentialsException("Invalid credentials"));

        if (!passwordMatches(usuario.getPassword(), password)) {
            int currentFailures = failedAttempts.getOrDefault(correo, 0) + 1;
            failedAttempts.put(correo, currentFailures);

            if (currentFailures >= MAX_FAILED_ATTEMPTS) {
                lockoutExpiry.put(correo, now + LOCKOUT_DURATION_MILLIS);
                throw new ResourceBlockedException(
                        "La cuenta ha sido bloqueada temporalmente por haber superado el maximo de 5 intentos fallidos."
                );
            }

            throw new InvalidCredentialsException("Invalid credentials");
        }

        // KAN-16: migracion transparente de contrasenas guardadas en texto plano
        if (!isBcryptHash(usuario.getPassword())) {
            usuario.setPassword(passwordEncoder.encode(password));
            usuarioRepository.save(usuario);
        }

        // Si la autenticacion fue exitosa, limpiamos intentos fallidos acumulados
        failedAttempts.remove(correo);
        lockoutExpiry.remove(correo);
        return true;
    }

    private boolean passwordMatches(String stored, String raw) {
        if (stored == null || raw == null || OAUTH_PLACEHOLDER.equals(stored)) {
            return false;
        }
        if (isBcryptHash(stored)) {
            return passwordEncoder.matches(raw, stored);
        }
        // Legacy plaintext row: constant-time comparison, rehashed by the caller on success
        return MessageDigest.isEqual(stored.getBytes(StandardCharsets.UTF_8), raw.getBytes(StandardCharsets.UTF_8));
    }

    private static boolean isBcryptHash(String stored) {
        return stored != null && stored.matches("^\\$2[aby]\\$\\d{2}\\$.{53}$");
    }

    public void registerUsuario(Usuario usuario) {
        // KAN-17: el rol nunca se toma del cliente
        usuario.setRol(Rol.USER);
        if (usuarioRepository.findByEmail(usuario.getEmail()).isPresent()) {
            throw new AlreadyExistsException("User with email " + usuario.getEmail() + " already exists");
        }
        if (usuario.getNombre() == null || usuario.getNombre().isBlank()) {
            throw new InvalidDataException("Name cannot be empty");
        }
        if (usuario.getEmail() == null || usuario.getEmail().isBlank()) {
            throw new InvalidDataException("Email cannot be empty");
        }
        if (usuario.getPassword() == null || usuario.getPassword().isBlank()) {
            throw new InvalidDataException("Password cannot be empty");
        }
        usuario.setPassword(passwordEncoder.encode(usuario.getPassword()));
        usuarioRepository.save(usuario);
    }

    public void editProfileName(int idUsuario, String nombre) {
        if (nombre == null || nombre.isBlank()) {
            throw new InvalidDataException("Name cannot be empty");
        }
        Usuario usuarioEditado = usuarioRepository.findById(idUsuario)
                .orElseThrow(() -> new NotFoundException("User with ID " + idUsuario + " not found"));
        if (!usuarioEditado.getNombre().equals(nombre)) {
            usuarioEditado.setNombre(nombre);
        }
        usuarioRepository.save(usuarioEditado);
    }

    public void editPassword(int idUsuario, String newPassword) {
        if (newPassword == null || newPassword.isBlank()) {
            throw new InvalidDataException("Password cannot be empty");
        }
        Usuario usuarioEditado = usuarioRepository.findById(idUsuario)
                .orElseThrow(() -> new NotFoundException("User with ID " + idUsuario + " not found"));
        usuarioEditado.setPassword(passwordEncoder.encode(newPassword));
        usuarioRepository.save(usuarioEditado);
    }

    /** KAN-18: the current password is required to set a new one. */
    public void changePassword(int idUsuario, String currentPassword, String newPassword) {
        if (newPassword == null || newPassword.isBlank()) {
            throw new InvalidDataException("Password cannot be empty");
        }
        Usuario usuario = usuarioRepository.findById(idUsuario)
                .orElseThrow(() -> new NotFoundException("User with ID " + idUsuario + " not found"));
        if (!passwordMatches(usuario.getPassword(), currentPassword)) {
            throw new InvalidCredentialsException("Invalid credentials");
        }
        usuario.setPassword(passwordEncoder.encode(newPassword));
        usuarioRepository.save(usuario);
    }

    public void resetLockout(String email) {
        failedAttempts.remove(email);
        lockoutExpiry.remove(email);
    }
}
