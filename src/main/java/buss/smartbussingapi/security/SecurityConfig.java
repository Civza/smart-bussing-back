package buss.smartbussingapi.security;

import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.jwk.source.JWKSourceBuilder;
import com.nimbusds.jose.proc.SecurityContext;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationManagerResolver;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationProvider;
import org.springframework.security.oauth2.server.resource.authentication.JwtIssuerAuthenticationManagerResolver;
import org.springframework.security.web.SecurityFilterChain;

import java.net.MalformedURLException;
import java.net.URI;
import java.util.Map;

import static org.springframework.http.HttpMethod.GET;
import static org.springframework.http.HttpMethod.OPTIONS;
import static org.springframework.http.HttpMethod.PATCH;
import static org.springframework.http.HttpMethod.POST;

/**
 * Single source of truth for who can call what (KAN-21 / KAN-22 / KAN-29). Anything not listed is denied.
 * Tokens are accepted from two issuers, chosen by the {@code iss} claim: Supabase (Google OAuth, dashboard)
 * and this backend (email/password login, mobile).
 */
@Configuration
@EnableConfigurationProperties(SecurityProperties.class)
public class SecurityConfig {

    private static final String API = "/api/v1";

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http,
                                                   AuthenticationManagerResolver<HttpServletRequest> authenticationManagerResolver,
                                                   JsonSecurityErrorHandlers errorHandlers) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable) // stateless bearer-token API, no cookies
                .cors(Customizer.withDefaults())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .httpBasic(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .logout(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/error").permitAll()
                        .requestMatchers(OPTIONS, "/**").permitAll()
                        // Public transit data and public forms
                        .requestMatchers(GET, API + "/ruta", API + "/ruta/*", API + "/ruta/coordenadas/*").permitAll()
                        .requestMatchers(GET, API + "/parada", API + "/parada/*").permitAll()
                        .requestMatchers(GET, API + "/lugar", API + "/lugar/*").permitAll()
                        .requestMatchers(GET, API + "/reporteRuta", API + "/reporteRuta/*").permitAll()
                        .requestMatchers(API + "/viaje", API + "/viaje/**").permitAll()
                        .requestMatchers(POST, API + "/interesado", API + "/registrarLugar").permitAll()
                        .requestMatchers(POST, API + "/user", API + "/user/login").permitAll()
                        // Any signed-in user, acting only on themselves
                        .requestMatchers(GET, API + "/user/me").authenticated()
                        .requestMatchers(PATCH, API + "/user/me/name", API + "/user/me/password").authenticated()
                        .requestMatchers(POST, API + "/reporteRuta/*").authenticated()
                        // Administration
                        .requestMatchers(POST, API + "/ruta", API + "/parada").hasRole("ADMIN")
                        .requestMatchers(GET, API + "/user", API + "/user/*", API + "/interesado").hasRole("ADMIN")
                        .anyRequest().denyAll())
                .oauth2ResourceServer(oauth2 -> oauth2
                        .authenticationManagerResolver(authenticationManagerResolver)
                        .authenticationEntryPoint(errorHandlers.entryPoint())
                        .accessDeniedHandler(errorHandlers.accessDeniedHandler()))
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint(errorHandlers.entryPoint())
                        .accessDeniedHandler(errorHandlers.accessDeniedHandler()));
        return http.build();
    }

    /** Fetched lazily and cached; overridden with an in-memory key set in tests. */
    @Bean
    public JWKSource<SecurityContext> supabaseJwkSource(SecurityProperties properties) throws MalformedURLException {
        return JWKSourceBuilder.create(URI.create(properties.supabase().jwksUri()).toURL()).build();
    }

    @Bean
    public AuthenticationManagerResolver<HttpServletRequest> authenticationManagerResolver(
            SecurityProperties properties,
            @Qualifier("supabaseJwkSource") JWKSource<SecurityContext> supabaseJwkSource,
            JwtTokenService jwtTokenService,
            UsuarioRoleConverter roleConverter) {
        JwtDecoder supabaseDecoder = SupabaseJwtDecoders.create(supabaseJwkSource, properties.supabase().issuer());
        Map<String, AuthenticationManager> managers = Map.of(
                properties.supabase().issuer(), managerFor(supabaseDecoder, roleConverter),
                jwtTokenService.getIssuer(), managerFor(jwtTokenService.appJwtDecoder(), roleConverter));
        // Unknown issuers resolve to null, which Spring Security rejects as 401 invalid_token.
        return new JwtIssuerAuthenticationManagerResolver(managers::get);
    }

    private static AuthenticationManager managerFor(JwtDecoder decoder, UsuarioRoleConverter roleConverter) {
        JwtAuthenticationProvider provider = new JwtAuthenticationProvider(decoder);
        provider.setJwtAuthenticationConverter(roleConverter);
        return provider::authenticate;
    }
}
