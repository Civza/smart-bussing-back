package buss.smartbussingapi.security;

import buss.smartbussingapi.Usuario.UsuarioRepository;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Turns a verified JWT (Supabase or our own) into an authentication whose authority is the
 * caller's current {@code usuario.rol}. Identity is the token's {@code email} claim for both issuers.
 */
@Component
public class UsuarioRoleConverter implements Converter<Jwt, AbstractAuthenticationToken> {

    public static final String EMAIL_CLAIM = "email";

    private final UsuarioRepository usuarioRepository;

    public UsuarioRoleConverter(UsuarioRepository usuarioRepository) {
        this.usuarioRepository = usuarioRepository;
    }

    @Override
    public AbstractAuthenticationToken convert(Jwt jwt) {
        String email = jwt.getClaimAsString(EMAIL_CLAIM);
        List<GrantedAuthority> authorities = email == null ? List.of() : usuarioRepository.findByEmail(email)
                .map(usuario -> List.<GrantedAuthority>of(new SimpleGrantedAuthority("ROLE_" + usuario.getRol().name())))
                .orElse(List.of());
        return new JwtAuthenticationToken(jwt, authorities, email);
    }
}
