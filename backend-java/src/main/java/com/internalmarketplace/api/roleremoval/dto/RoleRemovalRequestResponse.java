package com.internalmarketplace.api.roleremoval.dto;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import com.google.cloud.Timestamp;
import com.google.cloud.firestore.DocumentSnapshot;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record RoleRemovalRequestResponse(
        String id,
        String requestedByUid,
        String requestedByName,
        String requestedByEmail,
        String status,
        String reviewedBy,
        String reviewNote,
        Timestamp createdAt,
        Timestamp updatedAt
) {
    public static RoleRemovalRequestResponse fromSnapshot(DocumentSnapshot doc) {
        return new RoleRemovalRequestResponse(
                doc.getId(),
                doc.getString("requested_by"),
                doc.getString("requested_by_name"),
                doc.getString("requested_by_email"),
                doc.getString("status"),
                doc.getString("reviewed_by"),
                doc.getString("review_note"),
                doc.getTimestamp("created_at"),
                doc.getTimestamp("updated_at"));
    }
}
