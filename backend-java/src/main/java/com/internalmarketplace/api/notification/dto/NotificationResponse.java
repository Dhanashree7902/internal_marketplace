package com.internalmarketplace.api.notification.dto;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import com.google.cloud.Timestamp;
import com.google.cloud.firestore.DocumentSnapshot;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record NotificationResponse(
        String id,
        String userId,
        String type,
        String referenceId,
        String message,
        Timestamp readAt,
        Timestamp createdAt
) {
    public static NotificationResponse fromSnapshot(DocumentSnapshot doc) {
        return new NotificationResponse(
                doc.getId(),
                doc.getString("user_id"),
                doc.getString("type"),
                doc.getString("reference_id"),
                doc.getString("message"),
                doc.getTimestamp("read_at"),
                doc.getTimestamp("created_at"));
    }
}
