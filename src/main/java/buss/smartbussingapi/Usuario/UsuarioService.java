package buss.smartbussingapi.Usuario;

import buss.smartbussingapi.DTOs.UsuarioDTO;
import buss.smartbussingapi.commons.exceptions.AlreadyExistsException;
import buss.smartbussingapi.commons.exceptions.InvalidCredentialsException;
import buss.smartbussingapi.commons.exceptions.InvalidDataException;
import buss.smartbussingapi.commons.exceptions.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class UsuarioService {

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
        Usuario usuario = usuarioRepository.findByEmail(correo)
                .orElseThrow(() -> new InvalidCredentialsException("Invalid credentials"));
        if (!usuario.getPassword().equals(password)) {
            throw new InvalidCredentialsException("Invalid credentials");
        }
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
}