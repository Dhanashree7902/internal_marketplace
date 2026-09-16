package com.internalmarketplace.api.report;

import com.google.cloud.firestore.CollectionReference;
import com.google.cloud.firestore.DocumentReference;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.FieldValue;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.QuerySnapshot;
import com.internalmarketplace.api.common.firestore.FirestoreCollections;
import com.internalmarketplace.api.common.firestore.FirestoreSupport;
import com.internalmarketplace.api.report.dto.ReportResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Thin Firestore wrapper, a direct analogue of reports.service.js. */
@Repository
@RequiredArgsConstructor
public class ReportRepository {

    private final Firestore firestore;

    private CollectionReference collection() {
        return firestore.collection(FirestoreCollections.REPORTS);
    }

    public String create(String reporterId, String reporterEmail, String reporterName, String targetType,
                          String targetId, String reason) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("reporter_id", reporterId);
        data.put("reporter_email", reporterEmail);
        data.put("reporter_name", reporterName);
        data.put("target_type", targetType);
        data.put("target_id", targetId);
        data.put("reason", reason);
        data.put("status", "PENDING");
        data.put("reviewed_by", null);
        data.put("created_at", FieldValue.serverTimestamp());
        data.put("updated_at", FieldValue.serverTimestamp());

        DocumentReference ref = FirestoreSupport.await(collection().add(data));
        return ref.getId();
    }

    public List<ReportResponse> listPending() {
        QuerySnapshot snap = FirestoreSupport.await(collection().whereEqualTo("status", "PENDING").orderBy("created_at").get());
        return snap.getDocuments().stream().map(ReportResponse::fromSnapshot).toList();
    }

    public Optional<ReportResponse> findById(String id) {
        DocumentSnapshot doc = FirestoreSupport.await(collection().document(id).get());
        return doc.exists() ? Optional.of(ReportResponse.fromSnapshot(doc)) : Optional.empty();
    }

    public void updateReview(String id, String status, String reviewedBy) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("status", status);
        data.put("reviewed_by", reviewedBy);
        data.put("updated_at", FieldValue.serverTimestamp());
        FirestoreSupport.await(collection().document(id).update(data));
    }
}
