package com.internalmarketplace.api.notification;

import com.google.cloud.firestore.CollectionReference;
import com.google.cloud.firestore.DocumentReference;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.FieldValue;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.Query;
import com.google.cloud.firestore.QuerySnapshot;
import com.google.cloud.firestore.WriteBatch;
import com.internalmarketplace.api.common.firestore.FirestoreCollections;
import com.internalmarketplace.api.common.firestore.FirestoreSupport;
import com.internalmarketplace.api.notification.dto.NotificationResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Thin Firestore wrapper, a direct analogue of notifications.service.js. */
@Repository
@RequiredArgsConstructor
public class NotificationRepository {

    private final Firestore firestore;

    private CollectionReference collection() {
        return firestore.collection(FirestoreCollections.NOTIFICATIONS);
    }

    public List<NotificationResponse> listForUser(String userId) {
        QuerySnapshot snap = FirestoreSupport.await(
                collection().whereEqualTo("user_id", userId)
                        .orderBy("created_at", Query.Direction.DESCENDING)
                        .limit(50)
                        .get());
        return snap.getDocuments().stream().map(NotificationResponse::fromSnapshot).toList();
    }

    public Optional<NotificationResponse> findById(String id) {
        DocumentSnapshot doc = FirestoreSupport.await(collection().document(id).get());
        return doc.exists() ? Optional.of(NotificationResponse.fromSnapshot(doc)) : Optional.empty();
    }

    public void markRead(String id) {
        FirestoreSupport.await(collection().document(id).update("read_at", FieldValue.serverTimestamp()));
    }

    public void delete(String id) {
        FirestoreSupport.await(collection().document(id).delete());
    }

    /** Single-recipient notification, used by category-request review and post moderation. */
    public String create(String userId, String type, String referenceId, String message) {
        DocumentReference ref = FirestoreSupport.await(collection().add(notificationData(userId, type, referenceId, message)));
        return ref.getId();
    }

    /** Batch fan-out to many recipients in one round trip, matching functions/src/index.js's notifyAdmins(). */
    public void createForMany(List<String> userIds, String type, String referenceId, String message) {
        if (userIds.isEmpty()) {
            return;
        }
        WriteBatch batch = firestore.batch();
        for (String userId : userIds) {
            batch.set(collection().document(), notificationData(userId, type, referenceId, message));
        }
        FirestoreSupport.await(batch.commit());
    }

    private static Map<String, Object> notificationData(String userId, String type, String referenceId, String message) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("user_id", userId);
        data.put("type", type);
        data.put("reference_id", referenceId);
        data.put("message", message);
        data.put("read_at", null);
        data.put("created_at", FieldValue.serverTimestamp());
        return data;
    }
}
