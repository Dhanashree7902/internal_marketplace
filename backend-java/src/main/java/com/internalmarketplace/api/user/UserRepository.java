package com.internalmarketplace.api.user;

import com.google.cloud.firestore.CollectionReference;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.FieldValue;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.QuerySnapshot;
import com.internalmarketplace.api.common.firestore.FirestoreCollections;
import com.internalmarketplace.api.common.firestore.FirestoreSupport;
import com.internalmarketplace.api.user.dto.EmployeeSummaryResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class UserRepository {

    private final Firestore firestore;

    private CollectionReference collection() {
        return firestore.collection(FirestoreCollections.USERS);
    }

    /** Backs the notification fan-out's {@code notifyAdmins()} (functions/src/index.js). */
    public List<String> findActiveAdminUids() {
        QuerySnapshot snap = FirestoreSupport.await(
                collection().whereEqualTo("role", "admin").whereEqualTo("status", "ACTIVE").get());
        return snap.getDocuments().stream().map(DocumentSnapshot::getId).toList();
    }

    public Optional<String> findEmailById(String uid) {
        DocumentSnapshot doc = FirestoreSupport.await(collection().document(uid).get());
        return doc.exists() ? Optional.ofNullable(doc.getString("email")) : Optional.empty();
    }

    public Optional<UserSummary> findSummaryById(String uid) {
        DocumentSnapshot doc = FirestoreSupport.await(collection().document(uid).get());
        if (!doc.exists()) {
            return Optional.empty();
        }
        return Optional.of(new UserSummary(doc.getString("email"), doc.getString("name")));
    }

    /** Candidates for promotion: active, non-admin accounts. Filtered further by search text in UserService. */
    public List<EmployeeSummaryResponse> listActiveEmployees() {
        QuerySnapshot snap = FirestoreSupport.await(
                collection().whereEqualTo("role", "employee").whereEqualTo("status", "ACTIVE").get());
        return snap.getDocuments().stream()
                .map(doc -> new EmployeeSummaryResponse(
                        doc.getId(),
                        doc.getString("name"),
                        doc.getString("email"),
                        doc.getString("employee_id"),
                        doc.getString("department")))
                .toList();
    }

    public Optional<EmployeeProfile> findProfileById(String uid) {
        DocumentSnapshot doc = FirestoreSupport.await(collection().document(uid).get());
        if (!doc.exists()) {
            return Optional.empty();
        }
        return Optional.of(new EmployeeProfile(
                doc.getId(), doc.getString("name"), doc.getString("email"), doc.getString("role"),
                doc.getString("status")));
    }

    public void updateRole(String uid, String role) {
        FirestoreSupport.await(collection().document(uid).update(
                "role", role,
                "updated_at", FieldValue.serverTimestamp()));
    }

    public record UserSummary(String email, String name) {
    }

    public record EmployeeProfile(String uid, String name, String email, String role, String status) {
    }
}
