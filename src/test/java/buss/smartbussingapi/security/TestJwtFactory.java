package buss.smartbussingapi.security;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.ECDSASigner;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jose.jwk.Curve;
import com.nimbusds.jose.jwk.ECKey;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.gen.ECKeyGenerator;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.SecurityContext;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.Map;

/**
 * Mints JWTs for tests without any network access.
 * Supabase tokens are ES256-signed with an in-memory key that tests expose through
 * {@link #supabaseJwkSource()}; app tokens are HS256-signed with the test secret.
 */
public final class TestJwtFactory {

    /** Must match smartbussing.security.jwt.secret in the test properties. */
    public static final String APP_SECRET = "test-only-secret-key-for-hs256-must-be-32-bytes-long!!";
    public static final String APP_ISSUER = "smart-bussing-api";
    /** Must match smartbussing.security.supabase.url + "/auth/v1" in the test properties. */
    public static final String SUPABASE_ISSUER = "https://test-project.supabase.co/auth/v1";

    private static final ECKey SUPABASE_KEY = generateEcKey("test-supabase-key");
    private static final ECKey FOREIGN_KEY = generateEcKey("attacker-key");

    private TestJwtFactory() {
    }

    public static JWKSource<SecurityContext> supabaseJwkSource() {
        return new ImmutableJWKSet<>(new JWKSet(SUPABASE_KEY.toPublicJWK()));
    }

    public static String supabaseToken(String email, String provider) {
        return supabaseToken(email, provider, SUPABASE_ISSUER, "authenticated", Instant.now().plusSeconds(3600), SUPABASE_KEY);
    }

    public static String supabaseTokenSignedByForeignKey(String email) {
        return supabaseToken(email, "google", SUPABASE_ISSUER, "authenticated", Instant.now().plusSeconds(3600), FOREIGN_KEY);
    }

    public static String expiredSupabaseToken(String email) {
        return supabaseToken(email, "google", SUPABASE_ISSUER, "authenticated", Instant.now().minusSeconds(3600), SUPABASE_KEY);
    }

    public static String supabaseTokenWithAudience(String email, String audience) {
        return supabaseToken(email, "google", SUPABASE_ISSUER, audience, Instant.now().plusSeconds(3600), SUPABASE_KEY);
    }

    public static String supabaseToken(String email, String provider, String issuer, String audience,
                                       Instant expiresAt, ECKey key) {
        JWTClaimsSet claims = new JWTClaimsSet.Builder()
                .issuer(issuer)
                .subject("8f7c1d2e-0000-4000-8000-" + Math.abs(email.hashCode()))
                .audience(audience)
                .claim("email", email)
                .claim("role", "authenticated")
                .claim("app_metadata", Map.of("provider", provider))
                .issueTime(Date.from(expiresAt.minusSeconds(3600)))
                .expirationTime(Date.from(expiresAt))
                .build();
        return sign(new JWSHeader.Builder(JWSAlgorithm.ES256).keyID(key.getKeyID()).build(), claims, key);
    }

    public static String appToken(int idUsuario, String email, String secret, String issuer, Instant expiresAt) {
        JWTClaimsSet claims = new JWTClaimsSet.Builder()
                .issuer(issuer)
                .subject(String.valueOf(idUsuario))
                .claim("email", email)
                .issueTime(Date.from(expiresAt.minusSeconds(3600)))
                .expirationTime(Date.from(expiresAt))
                .build();
        try {
            SignedJWT jwt = new SignedJWT(new JWSHeader(JWSAlgorithm.HS256), claims);
            jwt.sign(new MACSigner(secret.getBytes(StandardCharsets.UTF_8)));
            return jwt.serialize();
        } catch (JOSEException e) {
            throw new IllegalStateException(e);
        }
    }

    public static String expiredAppToken(int idUsuario, String email) {
        return appToken(idUsuario, email, APP_SECRET, APP_ISSUER, Instant.now().minusSeconds(60));
    }

    public static String appTokenSignedWithWrongSecret(int idUsuario, String email) {
        return appToken(idUsuario, email, "another-secret-that-is-also-32-bytes-long!!", APP_ISSUER,
                Instant.now().plusSeconds(3600));
    }

    public static String appTokenFromUnknownIssuer(int idUsuario, String email) {
        return appToken(idUsuario, email, APP_SECRET, "https://evil.example.com", Instant.now().plusSeconds(3600));
    }

    private static String sign(JWSHeader header, JWTClaimsSet claims, ECKey key) {
        try {
            SignedJWT jwt = new SignedJWT(header, claims);
            jwt.sign(new ECDSASigner(key));
            return jwt.serialize();
        } catch (JOSEException e) {
            throw new IllegalStateException(e);
        }
    }

    private static ECKey generateEcKey(String keyId) {
        try {
            return new ECKeyGenerator(Curve.P_256).keyID(keyId).generate();
        } catch (JOSEException e) {
            throw new IllegalStateException(e);
        }
    }
}
