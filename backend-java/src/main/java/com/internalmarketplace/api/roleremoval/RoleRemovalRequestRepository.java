package com.internalmarketplace.api.roleremoval;

import com.google.cloud.firestore.CollectionReference;
import com.google.cloud.firestore.DocumentReference;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.FieldValue;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.QuerySnapshot;
import com.internalmarketplace.api.common.firestore.FirestoreCollections;
import com.internalmarketplace.api.common.firestore.FirestoreSupport;
import com.internalmarketplace.api.roleremoval.dto.RoleRemovalRequestResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Thin Firestore wrapper for Admin self-service role-removal requests, mirrors CategoryRequestRepository. */
@Repository
@RequiredArgsConstructor
public class RoleRemovalRequestRepository {

    private final Firestore firestore;

    private CollectionReference collection() {
        return firestore.collection(FirestoreCollections.ROLE_REMOVAL_REQUESTS);
    }

    public String create(String requestedByUid, String requestedByName, String requestedByEmail) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("requested_by", requestedByUid);
        data.put("requested_by_name", requestedByName);
        data.put("requested_by_email", requestedByEmail);
        data.put("status", "PENDING");
        data.put("reviewed_by", null);
        data.put("review_note", "");
        data.put("created_at", FieldValue.serverTimestamp());
        data.put("updated_at", FieldValue.serverTimestamp());

        DocumentReference ref = FirestoreSupport.await(collection().add(data));
        return ref.getId();
    }

    public boolean existsPendingFor(String requestedByUid) {
        QuerySnapshot snap = FirestoreSupport.await(
                collection().whereEqualTo("status", "PENDING")
                        .whereEqualTo("requested_by", requestedByUid)
                        .limit(1)
                        .get());
        return !snap.isEmpty();
    }

    public List<RoleRemovalRequestResponse> listPending() {
        QuerySnapshot snap = FirestoreSupport.await(
                collection().whereEqualTo("status", "PENDING").orderBy("created_at").get());
        return snap.getDocuments().stream().map(RoleRemovalRequestResponse::fromSnapshot).toList();
    }

    public Optional<RoleRemovalRequestResponse> findById(String id) {
        DocumentSnapshot doc = FirestoreSupport.await(collection().document(id).get());
        return doc.exists() ? Optional.of(RoleRemovalRequestResponse.fromSnapshot(doc)) : Optional.empty();
    }

    public void updateDecision(String id, String status, String reviewedBy, String reviewNote) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("status", status);
        data.put("reviewed_by", reviewedBy);
        data.put("review_note", reviewNote == null ? "" : reviewNote);
        data.put("updated_at", FieldValue.serverTimestamp());
        FirestoreSupport.await(collection().document(id).update(data));
    }
}
