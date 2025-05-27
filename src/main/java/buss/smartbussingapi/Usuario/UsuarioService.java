package buss.smartbussingapi.Usuario;

import buss.smartbussingapi.DTOs.UsuarioDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class UsuarioService {

    @Autowired
    private final UsuarioRepository usuarioRepository;

    public UsuarioService(UsuarioRepository usuarioRepository) {
        this.usuarioRepository = usuarioRepository;
    }

    //Getter methods
    public List<Usuario> getUsuarios(){
        return usuarioRepository.findAll();
    }

    public Usuario getUsuarioById(int id_usuario){
        return usuarioRepository.findById(id_usuario).get();
    }

    public UsuarioDTO getUsuarioByEmail(String email){
        Optional<Usuario> usuario = usuarioRepository.findByEmail(email);

        if(!usuario.isPresent()){
            throw new IllegalArgumentException("No existe ese usuario");
        }

        Usuario usuarioMain = usuario.get();
        UsuarioDTO usuarioDTO = new UsuarioDTO();

        usuarioDTO.setNombre(usuarioMain.getNombre());
        usuarioDTO.setEmail(usuarioMain.getEmail());
        return usuarioDTO;
    }

    public int getUsuarioIdByEmail(String email) {
        Optional<Usuario> usuario = usuarioRepository.findByEmail(email);

        if(!usuario.isPresent()){
            throw new IllegalArgumentException("Usuario no encontrado");
        }

        return usuario.get().getId_usuario();
    }

    //bussines logic : LOGIN

    public boolean verifyCredential(String correo, String password){
        Optional<Usuario> usuario = usuarioRepository.findByEmail(correo);

        if(!usuario.isPresent()){
            throw new IllegalArgumentException("Las credendenciales no son iguales");
        }

        return usuario.get().getPassword().equals(password);
    }

    //Bussiness Logic : CLOSE SESSION

    // POST : Register

    public void registerUsuario(Usuario usuario){
        Optional<Usuario> usuarioExists = usuarioRepository.findByEmail(usuario.getEmail());

        if(usuarioExists.isPresent()){
            throw new IllegalArgumentException("Usuario ya existe");
        }

        if(usuario.getNombre().isEmpty()){
            throw new IllegalArgumentException("El nombre esta vacio");
        }

        if(usuario.getEmail().isEmpty()){
            throw new IllegalArgumentException("El email esta vacio");
        }

        if(usuario.getPassword().isEmpty()){
            throw new IllegalArgumentException("La contraseña esta vacio");
        }

        usuarioRepository.save(usuario);
    }

    //PATCH

    public void editProfileName(int id_usuario,String nombre) {
        Optional<Usuario> usuario = usuarioRepository.findById(id_usuario);

        if (!usuario.isPresent()) {
            throw new IllegalArgumentException("Usuario no encontrado");
        }

        Usuario usuarioEditado = usuario.get();

        if (!usuario.get().getNombre().equals(nombre)) {
            usuarioEditado.setNombre(nombre);
        }

        usuarioRepository.save(usuarioEditado);
    }


    public void editPassword(int id_usuario,String newPassword) {
        Optional<Usuario> usuario = usuarioRepository.findById(id_usuario);

        if (!usuario.isPresent()) {
            throw new IllegalArgumentException("Usuario no encontrado");
        }

        Usuario usuarioEditado = usuario.get();

        if (!usuario.get().getPassword().equals(newPassword)) {
            usuarioEditado.setPassword(newPassword);
        }
        usuarioRepository.save(usuarioEditado);
    }


}
