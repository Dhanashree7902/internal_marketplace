package com.internalmarketplace.api.security;

/**
 * The authenticated caller, loaded from Firestore users/{uid} on every
 * request (see FirebaseAuthenticationFilter) — analogous to Express's
 * {@code req.user}. The role is always read from the trusted Firestore
 * document, never from the ID token's custom claims, which can lag a role
 * change (same rationale as middleware/auth.js).
 */
public record FirebaseUserPrincipal(
        String uid,
        String email,
        String name,
        String employeeId,
        String department,
        String role,
        String status
) {
    public boolean isAdmin() {
        return "admin".equals(role);
    }
}
