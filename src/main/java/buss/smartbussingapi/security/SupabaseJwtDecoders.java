package buss.smartbussingapi.security;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.JWSVerificationKeySelector;
import com.nimbusds.jose.proc.SecurityContext;
import com.nimbusds.jwt.proc.DefaultJWTProcessor;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2ErrorCodes;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtIssuerValidator;
import org.springframework.security.oauth2.jwt.JwtTimestampValidator;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;

import java.util.Map;
import java.util.Set;

/** Builds the decoder for Supabase access tokens (asymmetric signing keys published as JWKS). */
final class SupabaseJwtDecoders {

    static final String AUDIENCE = "authenticated";
    /** Only identities whose email Google already verified are trusted (see providerIsGoogle). */
    static final String TRUSTED_PROVIDER = "google";

    private SupabaseJwtDecoders() {
    }

    static JwtDecoder create(JWKSource<SecurityContext> jwkSource, String issuer) {
        DefaultJWTProcessor<SecurityContext> processor = new DefaultJWTProcessor<>();
        processor.setJWSKeySelector(
                new JWSVerificationKeySelector<>(Set.of(JWSAlgorithm.ES256, JWSAlgorithm.RS256), jwkSource));
        // Claims are validated below by Spring's validators; disable Nimbus' own duplicate checks.
        processor.setJWTClaimsSetVerifier((claims, context) -> { });

        NimbusJwtDecoder decoder = new NimbusJwtDecoder(processor);
        decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(
                new JwtTimestampValidator(JwtTokenService.CLOCK_SKEW),
                new JwtIssuerValidator(issuer),
                audienceIsAuthenticated(),
                providerIsGoogle()));
        return decoder;
    }

    private static OAuth2TokenValidator<Jwt> audienceIsAuthenticated() {
        return jwt -> jwt.getAudience() != null && jwt.getAudience().contains(AUDIENCE)
                ? OAuth2TokenValidatorResult.success()
                : failure("Token audience is not '" + AUDIENCE + "'");
    }

    /**
     * Supabase email/password sign-ups can carry an unverified email (e.g. someone else's).
     * Accepting them would let anyone claim the admin's email, so only Google identities are trusted;
     * email/password users authenticate through POST /api/v1/user/login instead.
     */
    private static OAuth2TokenValidator<Jwt> providerIsGoogle() {
        return jwt -> {
            Object appMetadata = jwt.getClaims().get("app_metadata");
            if (appMetadata instanceof Map<?, ?> metadata && TRUSTED_PROVIDER.equals(metadata.get("provider"))) {
                return OAuth2TokenValidatorResult.success();
            }
            return failure("Supabase identity provider is not trusted");
        };
    }

    private static OAuth2TokenValidatorResult failure(String description) {
        return OAuth2TokenValidatorResult.failure(new OAuth2Error(OAuth2ErrorCodes.INVALID_TOKEN, description, null));
    }
}
