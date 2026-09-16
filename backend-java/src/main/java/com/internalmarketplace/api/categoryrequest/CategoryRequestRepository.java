package com.internalmarketplace.api.categoryrequest;

import com.google.cloud.firestore.CollectionReference;
import com.google.cloud.firestore.DocumentReference;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.FieldValue;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.QuerySnapshot;
import com.internalmarketplace.api.categoryrequest.dto.CategoryRequestResponse;
import com.internalmarketplace.api.common.firestore.FirestoreCollections;
import com.internalmarketplace.api.common.firestore.FirestoreSupport;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Thin Firestore wrapper, a direct analogue of categoryRequests.service.js. */
@Repository
@RequiredArgsConstructor
public class CategoryRequestRepository {

    private final Firestore firestore;

    private CollectionReference collection() {
        return firestore.collection(FirestoreCollections.CATEGORY_REQUESTS);
    }

    public boolean existsPendingByProposedName(String proposedName) {
        QuerySnapshot snap = FirestoreSupport.await(
                collection().whereEqualTo("status", "PENDING").whereEqualTo("proposed_name", proposedName).limit(1).get());
        return !snap.isEmpty();
    }

    public String create(String requestedBy, String proposedName, String reason) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("requested_by", requestedBy);
        data.put("proposed_name", proposedName);
        data.put("reason", reason);
        data.put("status", "PENDING");
        data.put("reviewed_by", null);
        data.put("review_note", "");
        data.put("created_at", FieldValue.serverTimestamp());
        data.put("updated_at", FieldValue.serverTimestamp());

        DocumentReference ref = FirestoreSupport.await(collection().add(data));
        return ref.getId();
    }

    public List<CategoryRequestResponse> listPending() {
        QuerySnapshot snap = FirestoreSupport.await(collection().whereEqualTo("status", "PENDING").orderBy("created_at").get());
        return snap.getDocuments().stream().map(CategoryRequestResponse::fromSnapshot).toList();
    }

    public Optional<CategoryRequestResponse> findById(String id) {
        DocumentSnapshot doc = FirestoreSupport.await(collection().document(id).get());
        return doc.exists() ? Optional.of(CategoryRequestResponse.fromSnapshot(doc)) : Optional.empty();
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
