package buss.smartbussingapi.security;

import buss.smartbussingapi.Usuario.Rol;
import buss.smartbussingapi.Usuario.Usuario;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;

import java.time.Duration;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

/** KAN-21 (T7): the backend signs its own short-lived HS256 tokens for email/password logins. */
class JwtTokenServiceTest {

    private SecurityProperties properties;
    private JwtTokenService tokenService;
    private Usuario usuario;

    @BeforeEach
    void setUp() {
        properties = new SecurityProperties(
                new SecurityProperties.AppJwt(TestJwtFactory.APP_SECRET, TestJwtFactory.APP_ISSUER, Duration.ofHours(1)),
                new SecurityProperties.Supabase("https://test-project.supabase.co"));
        tokenService = new JwtTokenService(properties);

        usuario = new Usuario();
        usuario.setIdUsuario(42);
        usuario.setEmail("pasajero@test.com");
        usuario.setRol(Rol.USER);
    }

    @Test
    void issuedTokenCarriesSubjectEmailIssuerAndOneHourExpiry() {
        String token = tokenService.issueToken(usuario);

        Jwt jwt = tokenService.appJwtDecoder().decode(token);
        assertEquals("42", jwt.getSubject());
        assertEquals("pasajero@test.com", jwt.getClaimAsString("email"));
        assertEquals(TestJwtFactory.APP_ISSUER, jwt.getClaimAsString("iss"));
        assertNotNull(jwt.getExpiresAt());
        assertNotNull(jwt.getIssuedAt());
        assertEquals(Duration.ofHours(1), Duration.between(jwt.getIssuedAt(), jwt.getExpiresAt()));
    }

    @Test
    void tokenDoesNotContainTheRoleSoRoleChangesApplyImmediately() {
        Jwt jwt = tokenService.appJwtDecoder().decode(tokenService.issueToken(usuario));
        assertNull(jwt.getClaims().get("rol"));
        assertNull(jwt.getClaims().get("role"));
    }

    @Test
    void ttlSecondsIsExposedForTheLoginResponse() {
        assertEquals(3600, tokenService.getTtlSeconds());
    }

    @Test
    void decoderRejectsTokenSignedWithAnotherSecret() {
        String forged = TestJwtFactory.appTokenSignedWithWrongSecret(42, "pasajero@test.com");
        JwtDecoder decoder = tokenService.appJwtDecoder();
        assertThrows(JwtException.class, () -> decoder.decode(forged));
    }

    @Test
    void decoderRejectsExpiredToken() {
        String expired = TestJwtFactory.appToken(42, "pasajero@test.com", TestJwtFactory.APP_SECRET,
                TestJwtFactory.APP_ISSUER, Instant.now().minusSeconds(120));
        JwtDecoder decoder = tokenService.appJwtDecoder();
        assertThrows(JwtException.class, () -> decoder.decode(expired));
    }

    @Test
    void decoderRejectsTokenWithForeignIssuer() {
        String foreign = TestJwtFactory.appTokenFromUnknownIssuer(42, "pasajero@test.com");
        JwtDecoder decoder = tokenService.appJwtDecoder();
        assertThrows(JwtException.class, () -> decoder.decode(foreign));
    }

    @Test
    void refusesToStartWithASecretShorterThan32Bytes() {
        SecurityProperties weak = new SecurityProperties(
                new SecurityProperties.AppJwt("too-short", TestJwtFactory.APP_ISSUER, Duration.ofHours(1)),
                new SecurityProperties.Supabase("https://test-project.supabase.co"));
        assertThrows(IllegalStateException.class, () -> new JwtTokenService(weak));
    }

    @Test
    void supabaseIssuerAndJwksUriAreDerivedFromProjectUrl() {
        assertEquals("https://test-project.supabase.co/auth/v1", properties.supabase().issuer());
        assertEquals("https://test-project.supabase.co/auth/v1/.well-known/jwks.json", properties.supabase().jwksUri());
    }
}
