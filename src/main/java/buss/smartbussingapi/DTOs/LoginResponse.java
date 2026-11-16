package buss.smartbussingapi.DTOs;

/** Bearer token issued by this backend plus the caller's public profile. */
public record LoginResponse(String token, String tokenType, long expiresIn, UsuarioDTO usuario) {

    public LoginResponse {
        usuario = copyOf(usuario);
    }

    public static LoginResponse bearer(String token, long expiresIn, UsuarioDTO usuario) {
        return new LoginResponse(token, "Bearer", expiresIn, usuario);
    }

    @Override
    public UsuarioDTO usuario() {
        return copyOf(usuario);
    }

    private static UsuarioDTO copyOf(UsuarioDTO source) {
        if (source == null) {
            return null;
        }
        UsuarioDTO copy = new UsuarioDTO();
        copy.setIdUsuario(source.getIdUsuario());
        copy.setNombre(source.getNombre());
        copy.setEmail(source.getEmail());
        copy.setUrlPhoto(source.getUrlPhoto());
        copy.setRol(source.getRol());
        return copy;
    }
}
