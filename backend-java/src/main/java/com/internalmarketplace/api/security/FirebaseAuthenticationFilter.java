package com.internalmarketplace.api.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.cloud.firestore.DocumentReference;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.FieldValue;
import com.google.cloud.firestore.Firestore;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.FirebaseToken;
import com.internalmarketplace.api.common.firestore.FirestoreCollections;
import com.internalmarketplace.api.common.firestore.FirestoreOperationException;
import com.internalmarketplace.api.common.firestore.FirestoreSupport;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Verifies the Firebase ID token on every request and loads the matching
 * users/{uid} Firestore doc so downstream handlers get a trusted role, not
 * one read off the token's custom claims. Direct port of middleware/auth.js.
 */
@Slf4j
public class FirebaseAuthenticationFilter extends OncePerRequestFilter {

    private final FirebaseAuth firebaseAuth;
    private final Firestore firestore;
    private final ObjectMapper objectMapper;
    private final String allowedWorkspaceDomain;
    private final String authEmulatorHost;
    private final boolean productionEnvironment;

    public FirebaseAuthenticationFilter(FirebaseAuth firebaseAuth, Firestore firestore, ObjectMapper objectMapper,
                                         String allowedWorkspaceDomain, String authEmulatorHost,
                                         boolean productionEnvironment) {
        this.firebaseAuth = firebaseAuth;
        this.firestore = firestore;
        this.objectMapper = objectMapper;
        this.allowedWorkspaceDomain = blankToNull(allowedWorkspaceDomain);
        this.authEmulatorHost = blankToNull(authEmulatorHost);
        this.productionEnvironment = productionEnvironment;
    }

    /**
     * Skips authentication entirely for the handful of routes SecurityConfig
     * marks permitAll (health checks, API docs) — otherwise this filter, which
     * runs before Spring Security's authorization rules are even evaluated,
     * would demand a bearer token on every request regardless of those rules
     * and break Cloud Run / load-balancer health checks.
     */
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return path.equals("/healthz")
                || path.startsWith("/actuator/health")
                || path.startsWith("/v3/api-docs")
                || path.startsWith("/swagger-ui");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String authHeader = request.getHeader("Authorization");
        String[] parts = authHeader != null ? authHeader.split(" ", 2) : new String[0];
        String scheme = parts.length > 0 ? parts[0] : null;
        String token = parts.length > 1 ? parts[1] : null;

        if (!"Bearer".equals(scheme) || token == null || token.isBlank()) {
            writeError(response, 401, "Missing bearer token", null);
            return;
        }

        try {
            FirebaseToken decoded = firebaseAuth.verifyIdToken(token);

            // When running against the local Auth emulator, skip the Google Workspace
            // domain check because emulator tokens won't include `hd`.
            if (authEmulatorHost == null && allowedWorkspaceDomain != null) {
                Object hd = decoded.getClaims().get("hd");
                if (!allowedWorkspaceDomain.equals(hd)) {
                    writeError(response, 403, "Account is outside the allowed company domain", null);
                    return;
                }
            }

            DocumentReference userRef = firestore.collection(FirestoreCollections.USERS).document(decoded.getUid());
            DocumentSnapshot userDoc = FirestoreSupport.await(userRef.get());

            if (!userDoc.exists()) {
                // First-time sign-in from an allowed account (domain check above already
                // passed): auto-provision as a plain employee instead of dead-ending on
                // an admin having to hand-create this doc.
                try {
                    Map<String, Object> newUser = new LinkedHashMap<>();
                    newUser.put("email", decoded.getEmail());
                    newUser.put("name", decoded.getName() != null ? decoded.getName() : decoded.getEmail());
                    newUser.put("employee_id", null);
                    newUser.put("department", null);
                    newUser.put("role", "employee");
                    newUser.put("status", "ACTIVE");
                    newUser.put("created_at", FieldValue.serverTimestamp());
                    FirestoreSupport.await(userRef.create(newUser));
                } catch (FirestoreOperationException e) {
                    if (!e.isAlreadyExists()) {
                        throw e;
                    }
                    // Lost a race to another concurrent first request (or an admin just
                    // created it manually) -- fall through and read what's there now.
                }
                userDoc = FirestoreSupport.await(userRef.get());
                if (!userDoc.exists()) {
                    writeError(response, 403, "No employee record for this account", null);
                    return;
                }
            }

            String status = userDoc.getString("status");
            if (!"ACTIVE".equals(status)) {
                writeError(response, 403, "Account is not active", null);
                return;
            }

            // Some accounts (e.g. created via scripts/createUser.js) only ever had
            // role/status stored, never email/name -- fall back to the verified
            // token claims so callers always get a usable email/name to display.
            String email = firstNonBlank(userDoc.getString("email"), decoded.getEmail());
            String name = firstNonBlank(userDoc.getString("name"), decoded.getName(), email);

            FirebaseUserPrincipal principal = new FirebaseUserPrincipal(
                    decoded.getUid(),
                    email,
                    name,
                    userDoc.getString("employee_id"),
                    userDoc.getString("department"),
                    userDoc.getString("role"),
                    status);

            List<GrantedAuthority> authorities = principal.isAdmin()
                    ? List.of(new SimpleGrantedAuthority("ROLE_ADMIN"), new SimpleGrantedAuthority("ROLE_EMPLOYEE"))
                    : List.of(new SimpleGrantedAuthority("ROLE_EMPLOYEE"));

            SecurityContextHolder.getContext().setAuthentication(
                    new UsernamePasswordAuthenticationToken(principal, null, authorities));

            filterChain.doFilter(request, response);
        } catch (FirebaseAuthException | FirestoreOperationException e) {
            log.debug("Authentication failed", e);
            writeError(response, 401, "Invalid or expired token", productionEnvironment ? null : e.getMessage());
        }
    }

    private void writeError(HttpServletResponse response, int status, String error, String details) throws IOException {
        response.setStatus(status);
        response.setContentType("application/json");
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("error", error);
        if (details != null) {
            body.put("details", details);
        }
        objectMapper.writeValue(response.getWriter(), body);
    }

    private static String blankToNull(String value) {
        return (value == null || value.isBlank()) ? null : value;
    }

    private static String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return values.length > 0 ? values[values.length - 1] : null;
    }
}
