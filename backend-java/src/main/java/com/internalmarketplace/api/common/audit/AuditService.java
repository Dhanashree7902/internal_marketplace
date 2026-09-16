package com.internalmarketplace.api.common.audit;

import com.google.cloud.Timestamp;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.FieldValue;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.Query;
import com.google.cloud.firestore.QuerySnapshot;
import com.internalmarketplace.api.common.firestore.FirestoreCollections;
import com.internalmarketplace.api.common.firestore.FirestoreSupport;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Records a privileged/moderation action per FR-15, a direct port of
 * lib/audit.js. Called synchronously by every admin-only mutation.
 */
@Component
@RequiredArgsConstructor
public class AuditService {

    private final Firestore firestore;

    public void recordAudit(String actorId, String action, String entityType, String entityId) {
        recordAudit(actorId, action, entityType, entityId, Map.of());
    }

    public void recordAudit(String actorId, String action, String entityType, String entityId,
                             Map<String, Object> metadata) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("actor_id", actorId);
        data.put("action", action);
        data.put("entity_type", entityType);
        data.put("entity_id", entityId);
        data.put("metadata", metadata == null ? Map.of() : metadata);
        data.put("created_at", FieldValue.serverTimestamp());

        FirestoreSupport.await(firestore.collection(FirestoreCollections.AUDIT_LOGS).add(data));
    }

    /** Reads back entries for a given action, newest first -- e.g. backing an admin-facing history panel. */
    public List<AuditEntry> listByAction(String action, int limit) {
        QuerySnapshot snap = FirestoreSupport.await(
                firestore.collection(FirestoreCollections.AUDIT_LOGS)
                        .whereEqualTo("action", action)
                        .orderBy("created_at", Query.Direction.DESCENDING)
                        .limit(limit)
                        .get());
        return snap.getDocuments().stream().map(AuditEntry::fromSnapshot).toList();
    }

    public record AuditEntry(String actorId, String entityId, Map<String, Object> metadata, Timestamp createdAt) {
        @SuppressWarnings("unchecked")
        static AuditEntry fromSnapshot(DocumentSnapshot doc) {
            Object metadata = doc.get("metadata");
            return new AuditEntry(
                    doc.getString("actor_id"),
                    doc.getString("entity_id"),
                    metadata instanceof Map ? (Map<String, Object>) metadata : Map.of(),
                    doc.getTimestamp("created_at"));
        }
    }
}
