package buss.smartbussingapi.security;

import buss.smartbussingapi.Usuario.Usuario;
import com.nimbusds.jose.jwk.source.ImmutableSecret;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwtIssuerValidator;
import org.springframework.security.oauth2.jwt.JwtTimestampValidator;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;

/**
 * Issues and verifies the HS256 tokens this backend signs for email/password logins (KAN-21).
 * The role is deliberately NOT a claim: it is read from the DB on every request, so a demotion
 * takes effect immediately instead of after the token expires.
 */
@Service
public final class JwtTokenService {

    /** Small tolerance for clock drift between Render and the clients. */
    static final Duration CLOCK_SKEW = Duration.ofSeconds(30);
    private static final int MIN_SECRET_BYTES = 32;

    private final String issuer;
    private final Duration ttl;
    private final JwtEncoder encoder;
    private final JwtDecoder decoder;

    public JwtTokenService(SecurityProperties properties) {
        SecurityProperties.AppJwt config = properties.jwt();
        byte[] secret = config.secret() == null ? new byte[0] : config.secret().getBytes(StandardCharsets.UTF_8);
        if (secret.length < MIN_SECRET_BYTES) {
            throw new IllegalStateException("APP_JWT_SECRET must be at least " + MIN_SECRET_BYTES + " bytes for HS256");
        }
        this.issuer = config.issuer();
        this.ttl = config.ttl();
        SecretKey key = new SecretKeySpec(secret, "HmacSHA256");
        this.encoder = new NimbusJwtEncoder(new ImmutableSecret<>(key));

        NimbusJwtDecoder nimbusDecoder = NimbusJwtDecoder.withSecretKey(key).macAlgorithm(MacAlgorithm.HS256).build();
        nimbusDecoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(
                new JwtTimestampValidator(CLOCK_SKEW), new JwtIssuerValidator(issuer)));
        this.decoder = nimbusDecoder;
    }

    public String issueToken(Usuario usuario) {
        Instant now = Instant.now();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(issuer)
                .subject(String.valueOf(usuario.getIdUsuario()))
                .claim("email", usuario.getEmail())
                .issuedAt(now)
                .expiresAt(now.plus(ttl))
                .build();
        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
        return encoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
    }

    public long getTtlSeconds() {
        return ttl.toSeconds();
    }

    public String getIssuer() {
        return issuer;
    }

    public JwtDecoder appJwtDecoder() {
        return decoder;
    }
}
