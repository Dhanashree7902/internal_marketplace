package com.internalmarketplace.api.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.cloud.firestore.Firestore;
import com.google.firebase.auth.FirebaseAuth;
import com.internalmarketplace.api.common.web.RateLimitFilter;
import com.internalmarketplace.api.security.FirebaseAuthenticationFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter;
import org.springframework.web.cors.CorsConfigurationSource;

import java.io.IOException;
import java.util.Map;

import static org.springframework.security.config.Customizer.withDefaults;

/**
 * Reduces the whole authorization model to two path rules, since every admin
 * route in the Node app is already prefixed with /admin (see
 * backend/src/modules/*\/*.routes.js). The security-header set approximates
 * helmet()'s defaults for a pure JSON API (no HTML is ever served, so a
 * strict default-src 'none' CSP is appropriate rather than a byte-for-byte
 * helmet-header replica).
 */
@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    private final FirebaseAuth firebaseAuth;
    private final Firestore firestore;
    private final ObjectMapper objectMapper;
    private final CorsConfigurationSource corsConfigurationSource;

    @Value("${app.allowed-workspace-domain:}")
    private String allowedWorkspaceDomain;

    @Value("${firebase.auth-emulator-host:}")
    private String authEmulatorHost;

    @Value("${app.environment:development}")
    private String environment;

    @Value("${app.rate-limit.capacity:30}")
    private int rateLimitCapacity;

    @Value("${app.rate-limit.refill-period-seconds:60}")
    private long rateLimitRefillSeconds;

    public SecurityConfig(FirebaseAuth firebaseAuth, Firestore firestore, ObjectMapper objectMapper,
                           CorsConfigurationSource corsConfigurationSource) {
        this.firebaseAuth = firebaseAuth;
        this.firestore = firestore;
        this.objectMapper = objectMapper;
        this.corsConfigurationSource = corsConfigurationSource;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        boolean productionEnvironment = "production".equalsIgnoreCase(environment);

        FirebaseAuthenticationFilter authenticationFilter = new FirebaseAuthenticationFilter(
                firebaseAuth, firestore, objectMapper, allowedWorkspaceDomain, authEmulatorHost,
                productionEnvironment);
        RateLimitFilter rateLimitFilter = new RateLimitFilter(rateLimitCapacity, rateLimitRefillSeconds, objectMapper);

        http
                .csrf(csrf -> csrf.disable())
                .cors(cors -> cors.configurationSource(corsConfigurationSource))
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .headers(headers -> headers
                        .contentSecurityPolicy(csp -> csp.policyDirectives("default-src 'none'"))
                        .frameOptions(frame -> frame.deny())
                        .httpStrictTransportSecurity(hsts -> hsts.includeSubDomains(true).maxAgeInSeconds(15_552_000))
                        .contentTypeOptions(withDefaults())
                        .referrerPolicy(referrer -> referrer.policy(ReferrerPolicyHeaderWriter.ReferrerPolicy.NO_REFERRER)))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/healthz", "/actuator/health", "/actuator/health/**").permitAll()
                        .requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()
                        .requestMatchers("/api/v1/admin/**").hasRole("ADMIN")
                        .requestMatchers("/api/v1/**").authenticated()
                        .anyRequest().denyAll())
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(this::handleUnauthenticated)
                        .accessDeniedHandler(this::handleAccessDenied))
                .addFilterBefore(authenticationFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(rateLimitFilter, FirebaseAuthenticationFilter.class);

        return http.build();
    }

    private void handleUnauthenticated(HttpServletRequest request, HttpServletResponse response, Exception ex)
            throws IOException {
        writeJsonError(response, 401, "Missing bearer token");
    }

    private void handleAccessDenied(HttpServletRequest request, HttpServletResponse response, Exception ex)
            throws IOException {
        writeJsonError(response, 403, "Admin role required");
    }

    private void writeJsonError(HttpServletResponse response, int status, String error) throws IOException {
        response.setStatus(status);
        response.setContentType("application/json");
        objectMapper.writeValue(response.getWriter(), Map.of("error", error));
    }
}
