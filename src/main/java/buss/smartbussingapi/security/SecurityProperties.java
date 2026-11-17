package buss.smartbussingapi.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/**
 * smartbussing.security.* — every value comes from environment variables in application.properties.
 *
 * @param jwt      tokens this backend signs for email/password logins
 * @param supabase Supabase project whose (Google OAuth) access tokens are trusted
 */
@ConfigurationProperties("smartbussing.security")
public record SecurityProperties(AppJwt jwt, Supabase supabase) {

    public record AppJwt(String secret, String issuer, Duration ttl) {
    }

    /** @param url project URL, e.g. https://abcd.supabase.co (issuer and JWKS are derived from it) */
    public record Supabase(String url) {

        public String issuer() {
            return stripTrailingSlash(url) + "/auth/v1";
        }

        public String jwksUri() {
            return issuer() + "/.well-known/jwks.json";
        }

        private static String stripTrailingSlash(String value) {
            if (value == null || value.isBlank()) {
                throw new IllegalStateException("smartbussing.security.supabase.url (SUPABASE_URL) is not configured");
            }
            return value.endsWith("/") ? value.substring(0, value.length() - 1) : value;
        }
    }
}
