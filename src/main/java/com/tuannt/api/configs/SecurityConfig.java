package com.tuannt.api.configs;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.jose.jwk.source.ImmutableSecret;
import com.tuannt.api.constants.ApiPaths;
import com.tuannt.api.constants.ApiStatus;
import com.tuannt.api.constants.UserRole;
import com.tuannt.api.exceptions.ApiError;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * Created by tuannt7 on 21/10/2026
 * <p>
 * Only the vocab write paths, saved words and account endpoints need a token; everything that
 * was public before this existed stays public. Rules live here as URL patterns rather than
 * {@code @PreAuthorize}, because a method-level AccessDeniedException would reach
 * GlobalExceptionHandler's Throwable handler and come back as a 500.
 */
@Configuration
@RequiredArgsConstructor
public class SecurityConfig {
    public static final String ROLE_CLAIM = "role";
    public static final String USERNAME_CLAIM = "username";
    private static final int MIN_SECRET_BYTES = 32;

    private final AuthConfig authConfig;
    private final ObjectMapper objectMapper;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        String admin = UserRole.ADMIN.name();
        return http
                // Stateless bearer tokens, no cookies, so CSRF protection has nothing to protect.
                .csrf(AbstractHttpConfigurer::disable)
                // Picks up the Spring MVC CORS mappings registered in GlobalConfig.
                .cors(Customizer.withDefaults())
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .httpBasic(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .logout(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers(ApiPaths.API_ADMIN_PATH + "/**").hasRole(admin)
                        .requestMatchers(HttpMethod.POST, ApiPaths.API_VOCAB_PATH + "/words/**").hasRole(admin)
                        .requestMatchers(HttpMethod.PUT, ApiPaths.API_VOCAB_PATH + "/words/**").hasRole(admin)
                        .requestMatchers(HttpMethod.DELETE, ApiPaths.API_VOCAB_PATH + "/words/**").hasRole(admin)
                        .requestMatchers(ApiPaths.API_VOCAB_PATH + "/saved/**").authenticated()
                        .requestMatchers(ApiPaths.API_AUTH_PATH + "/me").authenticated()
                        .requestMatchers(ApiPaths.API_AUTH_PATH + "/password").authenticated()
                        .anyRequest().permitAll())
                .oauth2ResourceServer(o -> o
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter()))
                        .authenticationEntryPoint((req, res, e) ->
                                writeError(res, HttpStatus.UNAUTHORIZED, ApiStatus.UNAUTHORIZED))
                        .accessDeniedHandler((req, res, e) ->
                                writeError(res, HttpStatus.FORBIDDEN, ApiStatus.FORBIDDEN)))
                .exceptionHandling(e -> e
                        .authenticationEntryPoint((req, res, ex) ->
                                writeError(res, HttpStatus.UNAUTHORIZED, ApiStatus.UNAUTHORIZED))
                        .accessDeniedHandler((req, res, ex) ->
                                writeError(res, HttpStatus.FORBIDDEN, ApiStatus.FORBIDDEN)))
                .build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public JwtEncoder jwtEncoder() {
        return new NimbusJwtEncoder(new ImmutableSecret<>(secretKey()));
    }

    @Bean
    public JwtDecoder jwtDecoder() {
        return NimbusJwtDecoder.withSecretKey(secretKey()).macAlgorithm(MacAlgorithm.HS256).build();
    }

    private SecretKey secretKey() {
        String secret = authConfig.getJwtSecret();
        if (secret == null || secret.getBytes(StandardCharsets.UTF_8).length < MIN_SECRET_BYTES) {
            throw new IllegalStateException("auth.jwtSecret (JWT_SECRET) must be at least "
                    + MIN_SECRET_BYTES + " bytes for HS256");
        }
        return new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
    }

    /** Maps the single "role" claim to ROLE_ADMIN / ROLE_USER for hasRole(). */
    private JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtGrantedAuthoritiesConverter authorities = new JwtGrantedAuthoritiesConverter();
        authorities.setAuthoritiesClaimName(ROLE_CLAIM);
        authorities.setAuthorityPrefix("ROLE_");
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(authorities);
        return converter;
    }

    /** Security rejections happen before the controller layer, so they bypass GlobalExceptionHandler. */
    private void writeError(HttpServletResponse response, HttpStatus status, ApiStatus apiStatus) throws IOException {
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(response.getOutputStream(), new ApiError(apiStatus));
    }
}
