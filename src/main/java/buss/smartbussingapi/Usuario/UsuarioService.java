package buss.smartbussingapi.Usuario;

import buss.smartbussingapi.DTOs.UsuarioDTO;
import buss.smartbussingapi.commons.exceptions.AlreadyExistsException;
import buss.smartbussingapi.commons.exceptions.InvalidCredentialsException;
import buss.smartbussingapi.commons.exceptions.InvalidDataException;
import buss.smartbussingapi.commons.exceptions.NotFoundException;
import buss.smartbussingapi.commons.exceptions.ResourceBlockedException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class UsuarioService {

    public static final int MAX_FAILED_ATTEMPTS = 5;
    public static final long LOCKOUT_DURATION_MILLIS = 15 * 60 * 1000L; // 15 minutos

    private final Map<String, Integer> failedAttempts = new ConcurrentHashMap<>();
    private final Map<String, Long> lockoutExpiry = new ConcurrentHashMap<>();

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
        usuarioDTO.setNombre(usuarioMain.getNombre());
        usuarioDTO.setEmail(usuarioMain.getEmail());
        usuarioDTO.setUrlPhoto(usuarioMain.getProfilePhotoURL());
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

        if (!usuario.getPassword().equals(password)) {
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

        // Si la autenticacion fue exitosa, limpiamos intentos fallidos acumulados
        failedAttempts.remove(correo);
        lockoutExpiry.remove(correo);
        return true;
    }

    public void registerUsuario(Usuario usuario) {
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
        if (!usuarioEditado.getPassword().equals(newPassword)) {
            usuarioEditado.setPassword(newPassword);
        }
        usuarioRepository.save(usuarioEditado);
    }

    public void resetLockout(String email) {
        failedAttempts.remove(email);
        lockoutExpiry.remove(email);
    }
}
